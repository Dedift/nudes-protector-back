package mm.nudesprotectorback.gallery.search

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.DateFormat
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.Instant

@Document(indexName = "images")
data class ImageDocument(
    @Id
    @Field(type = FieldType.Keyword)
    val id: String,

    @Field(type = FieldType.Text, analyzer = "standard")
    val title: String,

    @Field(type = FieldType.Text)
    val description: String? = null,

    @Field(type = FieldType.Keyword)
    val filename: String,

    @Field(type = FieldType.Keyword)
    val contentType: String,

    @Field(type = FieldType.Integer)
    val width: Int,

    @Field(type = FieldType.Integer)
    val height: Int,

    @Field(type = FieldType.Keyword)
    val tags: List<String> = emptyList(),

    @Field(type = FieldType.Date, format = [DateFormat.date_time])
    val uploadedAt: Instant,

    @Field(type = FieldType.Keyword)
    val uploadedBy: String,
)