/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.ormPanache;

import java.util.List;

import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PanacheBookRepository implements PanacheRepository<PanacheBook> {
	@Find
	public native List<PanacheBook> findBook(String isbn);

	@HQL("WHERE isbn = :isbn")
	public native List<PanacheBook> hqlBook(String isbn);
}
