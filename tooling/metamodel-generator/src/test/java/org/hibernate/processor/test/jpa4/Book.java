/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.jpa4;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeStatement;
import jakarta.persistence.NamedNativeStatements;
import jakarta.persistence.NamedStatement;
import jakarta.persistence.NamedStatements;

@NamedStatement(name = "updateBookTitle", statement = "update Book set title = 'updated'")
@NamedStatements(@NamedStatement(name = "deleteUnpublishedBooks", statement = "delete from Book where published = false"))
@NamedNativeStatement(name = "nativeBookDelete", statement = "delete from Book where published = false")
@NamedNativeStatements(@NamedNativeStatement(name = "native-book-update", statement = "update Book set title = 'updated'"))
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
