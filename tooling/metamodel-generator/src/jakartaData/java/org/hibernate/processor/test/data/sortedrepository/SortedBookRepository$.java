/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.sortedrepository;

import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.restrict.Restriction;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import org.hibernate.processor.test.data.namedquery.Book;

import java.util.List;
import java.util.stream.Stream;

@Repository
public interface SortedBookRepository$ extends SortedBookRepository {

	@Override
	@Query("from Book where pages > ?1")
	Stream<Book> withMorePagesThan(int minPages, Sort<? super Book>... sorts);

	@Override
	@Query("from Book where title = ?1")
	List<Book> withTitle(String title, Order<? super Book> order);

	@Override
	@Query("from Book where title = ?1")
	List<Book> byTitle(String title, Limit limit, Sort<? super Book>... sorts);

	@Override
	@Query("from Book")
	List<Book> books(Restriction<? super Book> restriction);
}
