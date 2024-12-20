/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.namedquery;

public final class TitleAndIsbn {
	private final String title;
	private final String isbn;

	public TitleAndIsbn(String title, String isbn) {
		this.title = title;
		this.isbn = isbn;
	}

	public String title() {
		return title;
	}

	public String isbn() {
		return isbn;
	}
}
