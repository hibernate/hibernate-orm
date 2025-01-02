/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
