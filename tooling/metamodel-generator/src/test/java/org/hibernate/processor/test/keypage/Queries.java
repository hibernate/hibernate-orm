/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
