/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.restriction;

import org.hibernate.StatelessSession;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.query.Order;
import org.hibernate.query.Restriction;

import java.util.List;


public interface Bookshelf {
	StatelessSession session();
	@Find Book book(String isbn);
	@Find
	List<Book> books(Restriction<? super Book> restriction);
	@Find
	List<Book> books(Restriction<? super Book>... restrictions);
	@Find
	List<Book> books(Order<? super Book> order);
	@Find
	List<Book> books(Order<? super Book>... orders);
	@Find
	List<Book> books(List<Restriction<? super Book>> restrictions, List<Order<? super Book>> orders);
	@HQL("from Book")
	List<Book> books1(Restriction<? super Book> restriction);
	@HQL("from Book")
	List<Book> books2(Restriction<? super Book>... restrictions);
	@HQL("from Book")
	List<Book> books3(Order<? super Book> order);
	@HQL("from Book")
	List<Book> books4(Order<? super Book>... orders);
	@HQL("from Book")
	List<Book> book5(List<Restriction<? super Book>> restrictions, List<Order<? super Book>> orders);
}
