package mm.nudesprotectorback.gallery.service

import java.util.Base64
import java.util.Locale

object GalleryImageMetadata {
    fun encodeId(objectName: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(objectName.toByteArray())

    fun decodeId(id: String): String =
        String(Base64.getUrlDecoder().decode(id))

    fun filename(objectName: String): String =
        objectName.substringAfterLast('/')

    fun buildTitle(objectName: String): String {
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
