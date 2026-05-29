/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.sortedrepository;

import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.restrict.Restriction;
import jakarta.data.repository.Repository;
import org.hibernate.processor.test.data.namedquery.Book;

import java.util.List;
import java.util.stream.Stream;

@Repository
public interface SortedBookRepository {

	Stream<Book> withMorePagesThan(int minPages, Sort<? super Book>... sorts);

	List<Book> withTitle(String title, Order<? super Book> order);

	List<Book> byTitle(String title, Limit limit, Sort<? super Book>... sorts);

	List<Book> books(Restriction<? super Book> restriction);
}
