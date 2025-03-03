/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.reactive;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.NaturalId;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Set;

@Entity
public class Book {
	@Id
	String isbn;

	@NaturalId
	String title;

	@NaturalId
	LocalDate publicationDate;

	String text;

	@Enumerated(EnumType.STRING)
	@Basic(optional = false)
	Type type = Type.Book;

	@ManyToOne(optional = false)
	Publisher publisher;

	@ManyToMany(mappedBy = "books")
	Set<Author> authors;

	@Basic(optional = false)
	int pages ;

	BigDecimal price;
	BigInteger quantitySold;

	public Book(String isbn, String title, String text) {
		this.isbn = isbn;
		this.title = title;
		this.text = text;
	}

	protected Book() {}

	@Override
	public String toString() {
		return isbn + " : " + title + " [" + type + "]";
	}
}
