package mm.nudesprotectorback.gallery.web

import mm.nudesprotectorback.gallery.service.GalleryService
import mm.nudesprotectorback.gallery.web.dto.GalleryItemResponse
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.InputStreamResource
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/api/images")
class ImageController(
    private val galleryService: GalleryService,
) {
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    fun listImages(authentication: Authentication?): List<GalleryItemResponse> =
        galleryService.listItems(isAuthenticated(authentication))

    @GetMapping("/{id}/original")
    fun getOriginal(@PathVariable id: String): ResponseEntity<InputStreamResource> {
        val image = galleryService.openOriginal(id)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.contentType))
            .contentLength(image.size)
            .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePrivate())
            .header("X-Content-Type-Options", "nosniff")
            .body(InputStreamResource(image.stream))
    }

    @GetMapping("/{id}/blurred")
    fun getBlurred(@PathVariable id: String): ResponseEntity<ByteArrayResource> {
        val image = galleryService.openBlurred(id)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.contentType))
            .contentLength(image.bytes.size.toLong())
            .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
            .header("X-Content-Type-Options", "nosniff")
            .body(ByteArrayResource(image.bytes))
    }

    private fun isAuthenticated(authentication: Authentication?): Boolean =
        authentication != null && authentication.isAuthenticated && authentication !is AnonymousAuthenticationToken
}
