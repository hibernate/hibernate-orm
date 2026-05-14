/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.jpa4;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
class Book {
	@Id
	Long id;

	String title;

	boolean published;

	Integer pages;

	BigDecimal price;

	LocalDate publicationDate;

	Status status;

	Object payload;
}
