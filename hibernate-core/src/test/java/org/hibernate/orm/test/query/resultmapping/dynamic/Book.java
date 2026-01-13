/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.resultmapping.dynamic;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.Id;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;
import org.hibernate.annotations.NaturalId;

import java.time.LocalDate;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("FieldCanBeLocal")
@Entity(name = "Book")
@Table(name = "books")
@SqlResultSetMapping(name = "book-dto",
		classes = @ConstructorResult(targetClass = Book.class,
				columns = {
						@ColumnResult(name = "id", type = Integer.class),
						@ColumnResult(name = "name", type = String.class),
						@ColumnResult(name = "isbn", type = String.class),
						@ColumnResult(name = "published", type = LocalDate.class)
				}
		)
)
@SqlResultSetMapping(name = "book-drop-down",
		classes = @ConstructorResult(targetClass = DropDownItem.class,
				columns = {
						@ColumnResult(name = "id", type = Integer.class),
						@ColumnResult(name = "name", type = String.class)
				}
		)
)
@SqlResultSetMapping(name = "id",
		columns = @ColumnResult(name = "id", type = Integer.class)
)
@SqlResultSetMapping(name = "book-implicit",
		entities = @EntityResult(entityClass = Book.class)
)
@SqlResultSetMapping(name = "book-explicit",
		entities = @EntityResult(
				entityClass = Book.class,
				fields = {
						@FieldResult(name = "id", column = "id_"),
						@FieldResult(name = "name", column = "name_"),
						@FieldResult(name = "isbn", column = "isbn_"),
						@FieldResult(name = "published", column = "published_"),
				}
		)
)
public class Book {
	@Id
	private Integer id;
	private String name;
	@NaturalId
	private String isbn;
	private LocalDate published;

	public Book() {
	}

	public Book(Integer id, String name, String isbn, LocalDate published) {
		this.id = id;
		this.name = name;
		this.isbn = isbn;
		this.published = published;
	}
}
