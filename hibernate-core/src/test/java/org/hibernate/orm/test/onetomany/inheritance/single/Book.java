/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetomany.inheritance.single;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;


@Entity
public class Book extends Product {

	private String isbn;

	@ManyToOne
	private Library library;

	public Book() {
		super();
	}

	public Book(String inventoryCode, String isbn) {
		super(inventoryCode);
		this.isbn = isbn;
	}

	public String getIsbn() {
		return isbn;
	}

	public Library getLibrary() {
		return library;
	}

	public void setLibrary(Library library) {
		this.library = library;
	}
}
