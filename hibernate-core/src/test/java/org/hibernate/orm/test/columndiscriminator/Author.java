/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.columndiscriminator;

import java.util.*;

public class Author {
	private Long id;
	private String name;
	private String email;
	private List<Book> books = new ArrayList<>();

	public Author(String name, String email) {
		this.name = name;
		this.email = email;
	}

	protected Author() {
		// default
	}

	public Long id() {
		return id;
	}

	public String name() {
		return name;
	}

	public String email() {
		return email;
	}

	public List<Book> books() {
		return books;
	}

	public void addBook(Book book) {
		books.add(book);
	}
}
