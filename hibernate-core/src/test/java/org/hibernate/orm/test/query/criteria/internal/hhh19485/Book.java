/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria.internal.hhh19485;


import jakarta.persistence.*;

import java.util.Objects;

/**
 * @author Jan Schatteman
 */
@Entity
public class Book {

	@Id
	private int id;
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
	@JoinColumn(name = "isbn")
	private Isbn isbn;

	public Book(int id, Isbn isbn) {
		this.id = id;
		this.isbn = isbn;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Book income = (Book) o;
		return id == income.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
