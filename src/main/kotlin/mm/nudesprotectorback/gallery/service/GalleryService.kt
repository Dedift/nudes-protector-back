package mm.nudesprotectorback.gallery.service

import io.minio.GetObjectArgs
import io.minio.ListObjectsArgs
import io.minio.MinioClient
import io.minio.StatObjectArgs
import mm.nudesprotectorback.gallery.config.GalleryProperties
import mm.nudesprotectorback.gallery.web.dto.GalleryItemResponse
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
import java.util.Base64
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlin.math.PI
import kotlin.math.exp
import kotlin.random.Random

@Service
class GalleryService(
    private val minioClient: MinioClient,
    private val galleryProperties: GalleryProperties,
) {
    private val blurredCache = ConcurrentHashMap<String, GalleryImageContent>()

    fun listItems(authenticated: Boolean): List<GalleryItemResponse> =
        minioClient.listObjects(
            ListObjectsArgs.builder()
                .bucket(galleryProperties.bucket)
                .recursive(true)
                .build()
        )
            .asSequence()
            .map { result -> result.get() }
            .filterNot { it.isDir }
            .toList()
            .shuffled(Random.Default)
            .take(galleryProperties.maxItems)
            .map { item ->
                val objectName = item.objectName()
                val id = encodeId(objectName)
                GalleryItemResponse(
                    id = id,
                    title = buildTitle(objectName),
                    imageUrl = if (authenticated) {
                        "/api/images/$id/original"
                    } else {
                        "/api/images/$id/blurred"
                    },
                )
            }

    fun openOriginal(id: String): GalleryStreamContent {
        val objectName = decodeId(id)
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
                contentType = stat.contentType().takeUnless { it.isNullOrBlank() } ?: MediaType.APPLICATION_OCTET_STREAM_VALUE,
                size = stat.size(),
            )
        } catch (_: Exception) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found")
        }
    }

    fun openBlurred(id: String): GalleryImageContent {
        return blurredCache.computeIfAbsent(id) {
            val original = openOriginal(id)
            original.stream.use { stream ->
                val source = ImageIO.read(stream) ?: throw ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported image format")
                val blurred = blurImage(source)
                val format = imageFormat(original.contentType)
                val bytes = ByteArrayOutputStream().use { output ->
                    ImageIO.write(blurred, format, output)
                    output.toByteArray()
                }

                GalleryImageContent(
                    bytes = bytes,
                    contentType = original.contentType,
                )
            }
        }
    }

    private fun blurImage(source: BufferedImage): BufferedImage {
        val prepared = pixelate(source, factor = 28)

        val kernel = gaussianKernel(radius = 20, sigma = 10.0)
        val operation = ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null)
        val firstPass = operation.filter(prepared, null)
        val secondPass = operation.filter(firstPass, null)
        val thirdPass = operation.filter(secondPass, null)

        return darken(thirdPass, alpha = 0.22f)
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

    private fun encodeId(objectName: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(objectName.toByteArray())

    private fun decodeId(id: String): String =
        try {
            String(Base64.getUrlDecoder().decode(id))
        } catch (_: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found")
        }

    private fun buildTitle(objectName: String): String {
        val filename = objectName.substringAfterLast('/').substringBeforeLast('.', objectName)
        return filename
            .replace('-', ' ')
            .replace('_', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                token.replaceFirstChar { char ->
                    if (char.isLowerCase()) {
                        char.titlecase(Locale.getDefault())
                    } else {
                        char.toString()
                    }
                }
            }
            .ifBlank { objectName }
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
