/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria.internal.hhh19485;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.Objects;

/**
 * @author Jan Schatteman
 */
@Entity
//@Table(schema = "public", name = "t_isbn")
public class Isbn {

	@Id
	private String isbn;

	public Isbn(String isbn) {
		this.isbn = isbn;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		Isbn isbn1 = (Isbn) o;
		return Objects.equals(isbn, isbn1.isbn);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(isbn);
	}
}
