package mm.nudesprotectorback.gallery.search

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

interface ImageSearchRepository : ElasticsearchRepository<ImageDocument, String>
