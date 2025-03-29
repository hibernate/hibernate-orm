/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.mapkeycolumn;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Size;

import java.util.HashMap;
import java.util.Map;

import static jakarta.persistence.CascadeType.PERSIST;

@Entity
class Book {
	@Id
	@Size(min=10, max = 13)
	String isbn;

	@OneToMany(cascade = PERSIST,
			mappedBy = "isbn")
	@MapKeyColumn(name = "chapter_name")
	Map<String,Chapter> chapters;

	Book(String isbn) {
		this.isbn = isbn;
		chapters = new HashMap<>();
	}

	Book() {
	}
}

@Entity
class Chapter {
	@Id String isbn;
	@Id @Column(name = "chapter_name") String name;
	String text;

	Chapter(String isbn, String name, String text) {
		this.isbn = isbn;
		this.name = name;
		this.text = text;
	}

	Chapter() {
	}
}
