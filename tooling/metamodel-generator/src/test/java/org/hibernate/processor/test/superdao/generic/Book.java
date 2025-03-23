/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.superdao.generic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.NaturalId;
import org.hibernate.processor.test.hqlsql.Publisher;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Book {
	@Id String isbn;
	@NaturalId String title;
	String text;
	@NaturalId String authorName;
	@ManyToOne
	Publisher publisher;
	BigDecimal price;
	int pages;
	LocalDate publicationDate;
}
