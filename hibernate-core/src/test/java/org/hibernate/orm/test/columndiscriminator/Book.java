/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.columndiscriminator;

public class Book {
	private Long id;
	private String title;
	private BookDetails details;

	public Book(String title, BookDetails details) {
		this.title = title;
		this.details = details;
	}

	protected Book() {
		// default
	}

	public Long id() {
		return id;
	}

	public String title() {
		return title;
	}

	public BookDetails details() {
		return details;
	}
}
