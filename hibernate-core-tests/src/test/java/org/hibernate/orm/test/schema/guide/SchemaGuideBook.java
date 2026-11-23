package org.hibernate.orm.test.schema.guide;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GeneratedColumn;

/// Model used by the Schema Generation chapter's executable examples.
///
/// @author Steve Ebersole
//tag::schema-guide-model[]
@Entity
//tag::schema-guide-table[]
@Table(name = "schema_book",
		check = @CheckConstraint(name = "positive_edition", constraint = "edition > 0"),
		uniqueConstraints = @UniqueConstraint(name = "uk_schema_book_title_author", columnNames = { "title", "author" }),
		indexes = @Index(name = "idx_schema_book_author", columnList = "author, title"))
//end::schema-guide-table[]
public class SchemaGuideBook {
	@Id
	Long id;

	@Column(nullable = false, length = 100)
	String title;

	@Column(nullable = false, length = 100)
	String author;

	//tag::schema-guide-default[]
	@ColumnDefault("1")
	@Generated
	Integer edition;
	//end::schema-guide-default[]

	//tag::schema-guide-generated-column[]
	@GeneratedColumn("char_length(title)")
	Integer titleLength;
	//end::schema-guide-generated-column[]

	protected SchemaGuideBook() {
	}

	public SchemaGuideBook(Long id, String title, String author) {
		this.id = id;
		this.title = title;
		this.author = author;
	}
}
//end::schema-guide-model[]
