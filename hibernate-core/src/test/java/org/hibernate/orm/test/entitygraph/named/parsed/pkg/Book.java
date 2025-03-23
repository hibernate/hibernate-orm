/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.parsed.pkg;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
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
