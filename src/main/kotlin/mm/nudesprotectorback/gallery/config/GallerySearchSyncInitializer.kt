package mm.nudesprotectorback.gallery.config

import mm.nudesprotectorback.gallery.service.GallerySearchSyncService
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GallerySearchSyncInitializer {

    @Bean
    fun gallerySearchSyncRunner(syncService: GallerySearchSyncService) = ApplicationRunner {
        syncService.syncFromMinio()
    }
}
