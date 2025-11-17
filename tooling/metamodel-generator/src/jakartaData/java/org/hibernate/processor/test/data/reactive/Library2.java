/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.reactive;

import io.smallrye.mutiny.Uni;
import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.Page;
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
import org.hibernate.reactive.mutiny.Mutiny;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface Library2 {

	Uni<Mutiny.StatelessSession> session(); //required

	@Find
	Uni<Book> book(String isbn);

	@Find
	Uni<Optional<Book>> maybeBook(String isbn);

	@Find
	Uni<List<Book>> books(@By("isbn") List<String> isbns);

	@Find
	Uni<Book> book(String title, LocalDate publicationDate);

	@Find
	Uni<List<Book>> publications(Type type, Sort<Book> sort);

	@Find
	@OrderBy("title")
	Uni<List<Book>> booksByPublisher(String publisher_name);

	@Query("where title like :titlePattern")
	@OrderBy("title")
	Uni<List<Book>> booksByTitle(String titlePattern);

	// not required by Jakarta Data
	record BookWithAuthor(Book book, Author author) {}
	@Query("select b, a from Book b join b.authors a order by b.isbn, a.ssn")
	Uni<List<BookWithAuthor>> booksWithAuthors();

	@Insert
	Uni<Void> create(Book book);

	@Insert
	Uni<Void> create(Book[] book);

	@Update
	Uni<Void> update(Book book);

	@Update
	Uni<Void> update(Book[] books);

	@Delete
	Uni<Void> delete(Book book);

	@Delete
	Uni<Void> delete(Book[] book);

	@Save
	Uni<Void> upsert(Book book);

	@Find
	Uni<Author> author(String ssn);

	@Insert
	Uni<Void> create(Author author);

	@Update
	Uni<Void> update(Author author);

	@Insert
	Uni<Publisher[]> insertAll(Publisher[] publishers);

	@Delete
	Uni<Void> deleteAll(List<Publisher> publishers);

	@Save
	Uni<Publisher> save(Publisher publisher);

	@Delete
	Uni<Publisher> delete(Publisher publisher);

	@Find
	@OrderBy("isbn")
	Uni<Page<Book>> allBooks(PageRequest pageRequest);

	@Find
	@OrderBy("name")
	@OrderBy("address.city")
	Uni<List<Author>> allAuthors(Order<Author> order, Limit limit);

	@Find
	Uni<List<Author>> authorsByCity(@By("address.city") String city);

	@Find
	Uni<List<Author>> authorsByCityAndPostcode(String address_city, String address_postcode);
}
