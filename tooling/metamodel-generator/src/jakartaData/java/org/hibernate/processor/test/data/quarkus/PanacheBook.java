/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.quarkus;

import java.util.List;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class PanacheBook extends PanacheEntity {
	public @NaturalId String isbn;
	public @NaturalId String title;
	public @NaturalId String author;
	public String text;
	public int pages;

	@Find
	public static native List<PanacheBook> findBook(String isbn);

	@HQL("WHERE isbn = :isbn")
	public static native List<PanacheBook> hqlBook(String isbn);
}
