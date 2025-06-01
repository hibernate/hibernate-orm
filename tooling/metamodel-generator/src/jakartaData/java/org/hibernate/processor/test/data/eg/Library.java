/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.eg;

import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

@Transactional(rollbackOn = RuntimeException.class)
@Repository
public interface Library {

	@Find
	Book book(@NotNull String isbn);

	@Find
	List<Book> books(@By("isbn") @NotEmpty List<String> isbns);

	@Find
	Book book(@NotBlank String title, @NotNull LocalDate publicationDate);

	@Find
	List<Book> publications(@NotNull Type type, Sort<Book> sort);

	@Find
	@OrderBy("title")
	List<Book> booksByPublisher(@NotBlank String publisher_name);

	@Query("where title like :titlePattern")
	@OrderBy("title")
	List<Book> booksByTitle(String titlePattern);

	// not required by Jakarta Data
	record BookWithAuthor(Book book, Author author) {}
	@Query("select b, a from Book b join b.authors a order by b.isbn, a.ssn")
	List<BookWithAuthor> booksWithAuthors();

	@Insert
	void create(Book book);

	@Insert
	void create(Book[] book);

	@Update
	void update(Book book);

	@Update
	void update(Book[] books);

	@Delete
	void delete(Book book);

	@Delete
	void delete(Book[] book);

	@Save
	void upsert(Book book);

	@Find
	Author author(String ssn);

	@Insert
	void create(Author author);

	@Update
	void update(Author author);

	@Save
	void save(@Valid Publisher publisher);

	@Delete
	void delete(Publisher publisher);

	@Update
	void updateAll(Publisher... publishers);

	@Find
	@OrderBy("isbn")
	CursoredPage<Book> allBooks(PageRequest pageRequest);

	@Find
	@OrderBy("name")
	@OrderBy("address.city")
	List<Author> allAuthors(Order<Author> order, Limit limit);

	@Find
	List<Author> authorsByCity(@By("address.city") @NotBlank String city);

	@Find
	List<Author> authorsByCityAndPostcode(String address_city, String address_postcode);

	@Query("where type = org.hibernate.processor.test.data.eg.Type.Magazine")
	List<Book> magazines();

	@Query("where type = Journal")
	List<Book> journals();
}
