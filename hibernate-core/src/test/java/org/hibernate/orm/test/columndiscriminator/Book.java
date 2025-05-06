/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
