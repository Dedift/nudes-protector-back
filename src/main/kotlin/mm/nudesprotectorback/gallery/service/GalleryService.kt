package mm.nudesprotectorback.gallery.service

import io.minio.GetObjectArgs
import io.minio.ListObjectsArgs
import io.minio.MinioClient
import io.minio.StatObjectArgs
import mm.nudesprotectorback.gallery.config.GalleryProperties
import mm.nudesprotectorback.gallery.search.ImageDocument
import mm.nudesprotectorback.gallery.web.dto.GalleryItemResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.search
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.Instant
import java.util.Locale
import javax.imageio.ImageIO
import kotlin.math.PI
import kotlin.math.exp
import kotlin.random.Random

@Service
class GalleryService(
    private val minioClient: MinioClient,
    private val galleryProperties: GalleryProperties,
    private val elasticsearchOperations: ElasticsearchOperations,
) {
    fun listItems(authenticated: Boolean): List<GalleryItemResponse> {
        val documents = loadDocuments()

        return documents.map { document ->
            GalleryItemResponse(
                id = document.id,
                title = document.title,
                imageUrl = if (authenticated) {
                    "/api/images/${document.id}/original"
                } else {
                    "/api/images/${document.id}/blurred"
                },
            )
        }
    }

    private fun loadDocuments(): List<ImageDocument> {
        return runCatching {
            val seed = Instant.now().toEpochMilli().toString()
            val query = NativeQuery.builder()
                .withQuery { query ->
                    query.functionScore { functionScore ->
                        functionScore
                            .query { innerQuery ->
                                innerQuery.matchAll { matchAll -> matchAll }
                            }
                            .functions { function ->
                                function.randomScore { randomScore ->
                                    randomScore.seed(seed)
                                }
                            }
                    }
                }
                .withPageable(PageRequest.of(0, galleryProperties.maxItems))
                .build()

            elasticsearchOperations.search<ImageDocument>(query)
                .searchHits
                .map { it.content }
        }.getOrElse {
            listFromMinio()
        }
    }

    private fun listFromMinio(): List<ImageDocument> =
        minioClient.listObjects(
            ListObjectsArgs.builder()
                .bucket(galleryProperties.bucket)
                .recursive(true)
                .build()
        )
            .asSequence()
            .map { result -> result.get() }
            .filterNot { it.isDir }
            .sortedByDescending { it.lastModified() }
            .map { item ->
                ImageDocument(
                    id = GalleryImageMetadata.encodeId(item.objectName()),
                    title = GalleryImageMetadata.buildTitle(item.objectName()),
                    filename = GalleryImageMetadata.filename(item.objectName()),
                    contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                    width = 0,
                    height = 0,
                    uploadedAt = item.lastModified()?.toInstant() ?: Instant.EPOCH,
                    uploadedBy = "system",
                )
            }
            .toList()
            .shuffled(Random.Default)
            .take(galleryProperties.maxItems)

    fun openOriginal(id: String): GalleryStreamContent {
        val objectName = try {
            GalleryImageMetadata.decodeId(id)
        } catch (_: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found")
        }
        try {
            val stat = minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(galleryProperties.bucket)
                    .`object`(objectName)
                    .build()
            )
            val stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(galleryProperties.bucket)
                    .`object`(objectName)
                    .build()
            )

            return GalleryStreamContent(
                stream = stream,
                contentType = resolveContentType(
                    objectName = objectName,
                    contentType = stat.contentType(),
                ),
                size = stat.size(),
            )
        } catch (_: Exception) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found")
        }
    }

    fun openBlurred(id: String): GalleryImageContent {
        val original = openOriginal(id)
        return original.stream.use { stream ->
            val source = ImageIO.read(stream) ?: throw ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported image format")
            val blurred = blurImage(source)
            val format = imageFormat(original.contentType)
            val bytes = ByteArrayOutputStream().use { output ->
                ImageIO.write(blurred, format, output)
                output.toByteArray()
            }

            GalleryImageContent(
                bytes = bytes,
                contentType = when (format) {
                    "png" -> MediaType.IMAGE_PNG_VALUE
                    else -> MediaType.IMAGE_JPEG_VALUE
                },
            )
        }
    }

    private fun blurImage(source: BufferedImage): BufferedImage {
        val preview = resizeToMaxDimension(source, maxDimension = 900)
        val prepared = pixelate(preview, factor = 7)

        val kernel = gaussianKernel(radius = 3, sigma = 1.8)
        val operation = ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null)
        val softened = operation.filter(prepared, null)

        return darken(softened, alpha = 0.12f)
    }

    private fun resizeToMaxDimension(source: BufferedImage, maxDimension: Int): BufferedImage {
        val largestDimension = maxOf(source.width, source.height)
        if (largestDimension <= maxDimension) {
            return source
        }

        val scale = maxDimension.toDouble() / largestDimension.toDouble()
        val targetWidth = maxOf(1, (source.width * scale).toInt())
        val targetHeight = maxOf(1, (source.height * scale).toInt())

        val resized = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
        val graphics = resized.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null)
        graphics.dispose()

        return resized
    }

    private fun pixelate(source: BufferedImage, factor: Int): BufferedImage {
        val scaledWidth = maxOf(1, source.width / factor)
        val scaledHeight = maxOf(1, source.height / factor)

        val reduced = BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB)
        val reducedGraphics = reduced.createGraphics()
        reducedGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        reducedGraphics.drawImage(source, 0, 0, scaledWidth, scaledHeight, null)
        reducedGraphics.dispose()

        val enlarged = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_RGB)
        val enlargedGraphics = enlarged.createGraphics()
        enlargedGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        enlargedGraphics.drawImage(reduced, 0, 0, source.width, source.height, null)
        enlargedGraphics.dispose()

        return enlarged
    }

    private fun darken(source: BufferedImage, alpha: Float): BufferedImage {
        val output = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_RGB)
        val graphics = output.createGraphics()
        graphics.drawImage(source, 0, 0, null)
        graphics.color = Color(0f, 0f, 0f, alpha)
        graphics.fillRect(0, 0, source.width, source.height)
        graphics.dispose()
        return output
    }

    private fun gaussianKernel(radius: Int, sigma: Double): Kernel {
        val size = radius * 2 + 1
        val data = FloatArray(size * size)
        var sum = 0.0

        for (y in -radius..radius) {
            for (x in -radius..radius) {
                val index = (y + radius) * size + (x + radius)
                val exponent = -((x * x + y * y) / (2.0 * sigma * sigma))
                val value = exp(exponent) / (2.0 * PI * sigma * sigma)
                data[index] = value.toFloat()
                sum += value
            }
        }

        for (index in data.indices) {
            data[index] = (data[index] / sum).toFloat()
        }

        return Kernel(size, size, data)
    }

    private fun imageFormat(contentType: String): String =
        when (contentType.lowercase(Locale.ROOT)) {
            "image/png" -> "png"
            "image/webp" -> "png"
            else -> "jpg"
        }

    private fun resolveContentType(
        objectName: String,
        contentType: String?,
    ): String {
        val normalized = contentType?.trim().orEmpty()
        if (normalized.isNotBlank() && normalized != MediaType.APPLICATION_OCTET_STREAM_VALUE) {
            return normalized
        }

        return when (objectName.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
            "png" -> MediaType.IMAGE_PNG_VALUE
            "gif" -> MediaType.IMAGE_GIF_VALUE
            "webp" -> "image/webp"
            "jpg", "jpeg" -> MediaType.IMAGE_JPEG_VALUE
            else -> MediaType.APPLICATION_OCTET_STREAM_VALUE
        }
    }
}

data class GalleryStreamContent(
    val stream: InputStream,
    val contentType: String,
    val size: Long,
)

data class GalleryImageContent(
    val bytes: ByteArray,
    val contentType: String,
)
