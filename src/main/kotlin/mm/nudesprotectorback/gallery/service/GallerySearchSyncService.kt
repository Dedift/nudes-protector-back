package mm.nudesprotectorback.gallery.service

import io.minio.GetObjectArgs
import io.minio.ListObjectsArgs
import io.minio.MinioClient
import io.minio.StatObjectArgs
import mm.nudesprotectorback.gallery.config.GalleryProperties
import mm.nudesprotectorback.gallery.search.ImageDocument
import mm.nudesprotectorback.gallery.search.ImageSearchRepository
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.time.Instant
import javax.imageio.ImageIO

@Service
class GallerySearchSyncService(
    private val minioClient: MinioClient,
    private val galleryProperties: GalleryProperties,
    private val imageSearchRepository: ImageSearchRepository,
) {
    fun syncFromMinio() {
        val documents = minioClient.listObjects(
            ListObjectsArgs.builder()
                .bucket(galleryProperties.bucket)
                .recursive(true)
                .build()
        )
            .asSequence()
            .map { result -> result.get() }
            .filterNot { it.isDir }
            .map { item -> toDocument(item.objectName()) }
            .toList()

        imageSearchRepository.deleteAll()
        imageSearchRepository.saveAll(documents)
    }

    private fun toDocument(objectName: String): ImageDocument {
        val stat = minioClient.statObject(
            StatObjectArgs.builder()
                .bucket(galleryProperties.bucket)
                .`object`(objectName)
                .build()
        )

        val dimensions = minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(galleryProperties.bucket)
                .`object`(objectName)
                .build()
        ).use { stream ->
            val image = ImageIO.read(stream)
            if (image != null) {
                image.width to image.height
            } else {
                0 to 0
            }
        }

        return ImageDocument(
            id = GalleryImageMetadata.encodeId(objectName),
            title = GalleryImageMetadata.buildTitle(objectName),
            description = null,
            filename = GalleryImageMetadata.filename(objectName),
            contentType = stat.contentType().takeUnless { it.isNullOrBlank() } ?: MediaType.APPLICATION_OCTET_STREAM_VALUE,
            width = dimensions.first,
            height = dimensions.second,
            tags = emptyList(),
            uploadedAt = stat.lastModified()?.toInstant() ?: Instant.now(),
            uploadedBy = "system",
        )
    }
}
