/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.hrPanache;

import java.util.List;

import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

import io.smallrye.mutiny.Uni;

public interface QuarkusBookRepository {
	@Find
	public Uni<List<PanacheBook>> findBook(String isbn);

	@HQL("WHERE isbn = :isbn")
	public Uni<List<PanacheBook>> hqlBook(String isbn);

	@HQL("DELETE FROM PanacheBook")
	public Uni<Void> deleteAllBooksVoid();

	@HQL("DELETE FROM PanacheBook")
	public Uni<Integer> deleteAllBooksInt();
}
