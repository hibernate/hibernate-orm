/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.compliance;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.NaturalId;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "books")
public class Book {
	@Id
	String isbn;

	@NaturalId
	@Basic(optional = false)
	String title;

	@Basic(optional = false)
	String text;

	@NaturalId
	LocalDate publicationDate;

	BigDecimal price;

	int pages;

	public Book(String isbn, String title, String text) {
		this.isbn = isbn;
		this.title = title;
		this.text = text;
	}
	Book() {}
}
