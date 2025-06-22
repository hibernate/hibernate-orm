/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.ordercol;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ListIndexBase;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.PERSIST;

@Entity
class Book {
	@Id
	@Size(min=10, max = 13)
	String isbn;

	@OneToMany(cascade = PERSIST,
			mappedBy = "isbn")
	@OrderColumn(name = "page_number")
	@ListIndexBase(1)
	List<Page> pages;

	Book(String isbn) {
		this.isbn = isbn;
		pages = new ArrayList<>();
	}

	Book() {
	}
}

@Entity
class Page {
	@Id String isbn;
	@Id @Column(name = "page_number") int number;
	String text;

	Page(String isbn, int number, String text) {
		this.isbn = isbn;
		this.number = number;
		this.text = text;
	}

	Page() {
	}
}
