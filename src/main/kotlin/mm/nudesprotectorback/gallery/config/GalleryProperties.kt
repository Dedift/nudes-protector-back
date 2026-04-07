package mm.nudesprotectorback.gallery.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.gallery.minio")
data class GalleryProperties(
    val endpoint: String,
    val accessKey: String,
    val secretKey: String,
    val bucket: String,
    val maxItems: Int = 10,
)
