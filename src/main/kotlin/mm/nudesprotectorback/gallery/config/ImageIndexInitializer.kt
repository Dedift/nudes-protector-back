package mm.nudesprotectorback.gallery.config

import mm.nudesprotectorback.gallery.search.ImageDocument
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.indexOps

@Configuration
class ImageIndexInitializer {

    @Bean
    fun imageIndexRunner(operations: ElasticsearchOperations) = ApplicationRunner {
        val indexOps = operations.indexOps<ImageDocument>()
        if (!indexOps.exists()) {
            indexOps.create()
            indexOps.putMapping(indexOps.createMapping(ImageDocument::class.java))
        }
    }
}