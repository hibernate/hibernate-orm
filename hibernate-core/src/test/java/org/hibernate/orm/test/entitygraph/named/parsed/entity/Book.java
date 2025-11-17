/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.parsed.entity;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.NamedEntityGraph;

/**
 * @author Steve Ebersole
 */
@Entity(name = "Book")
@Table(name = "Book")
@NamedEntityGraph(name = "book-title-isbn", graph = "title, isbn")
@NamedEntityGraph(name = "book-title-isbn-author", graph = "title, isbn, author")
@NamedEntityGraph(name = "book-title-isbn-editor", graph = "title, isbn, editor")
public class Book {
	@Id
	private Integer id;
	private String title;
	@Embedded
	private Isbn isbn;

	@ManyToOne
	@JoinColumn(name = "author_fk")
	Person author;

	@ManyToOne
	@JoinColumn(name = "editor_fk")
	Person editor;
}
