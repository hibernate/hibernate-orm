/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.repository;

import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;
import org.hibernate.StatelessSession;
import org.hibernate.processor.test.integ.model.Book;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository {

	StatelessSession session();

	@Insert
	void insert(Book book);

	@Insert
	void insertAll(List<Book> books);

	@Update
	void update(Book book);

	@Delete
	void delete(Book book);

	@Save
	void save(Book book);

	@Find
	Book byIsbn(String isbn);

	@Find
	Optional<Book> maybeByIsbn(String isbn);

	@Find
	List<Book> byTitle(String title);

	@Query("from Book where title like :pattern")
	List<Book> titleLike(String pattern);

	@Query("from Book where pages > :minPages order by title")
	List<Book> booksWithMorePages(int minPages);

	@Query("select count(*) from Book")
	long countAll();

	@Delete
	void deleteByIsbn(String isbn);
}
