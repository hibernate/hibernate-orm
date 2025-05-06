/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.keypage;

import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.Pattern;
import org.hibernate.query.KeyedPage;
import org.hibernate.query.KeyedResultList;

import java.time.LocalDate;

public interface Queries {
	@HQL("where publicationDate > :minDate")
	KeyedResultList<Book> booksFromDate(LocalDate minDate, KeyedPage<Book> page);

	@Find
	KeyedResultList<Book> booksWithTitleLike(@Pattern String title, KeyedPage<Book> page);
}
