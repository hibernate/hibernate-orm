/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.dao;

import org.hibernate.StatelessSession;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.SQL;
import org.hibernate.processor.test.integ.model.Book;

import java.util.List;

public interface BookDao {

	StatelessSession getSession();

	@Find
	Book getBook(String isbn);

	@Find
	List<Book> getBooksByTitle(String title);

	@Find
	Book getBookByTitleAndAuthor(String title, String author);

	@HQL("from Book where title like ?1")
	List<Book> findBooksByTitle(String title);

	@HQL("from Book where pages > :minPages order by title")
	List<Book> findBooksWithMorePages(int minPages);

	@HQL("select count(*) from Book")
	long countBooks();

	@HQL("from Book where isbn = :isbn")
	Book findByIsbn(String isbn);

	@org.jetbrains.annotations.Nullable
	@HQL("from Book where isbn = :isbn")
	Book findByIsbnNullable(String isbn);

	@HQL("delete from Book where isbn = :isbn")
	int deleteByIsbn(String isbn);

	@SQL("select * from integ_books where isbn = :isbn")
	Book findByIsbnNative(String isbn);

	@SQL("select count(*) from integ_books")
	long countBooksNative();
}
