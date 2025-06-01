/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.basic;

import jakarta.annotation.Nullable;
import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.PageRequest;
import jakarta.data.page.Page;
import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;
import org.hibernate.StatelessSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Repository(dataStore = "myds")
public interface BookAuthorRepository {

	StatelessSession session();

	@Delete
	int deleteByIdIn(String[] isbn);

	@Delete
	void deleteById(String isbn);

	@Insert
	void insertBooks0(Book[] books);

	@Insert
	void insertBooks1(List<Book> books);

	@Insert
	List<Book> insertBooks2(List<Book> books);

	@Insert
	Book[] insertBooks3(Book[] books);

	@Find
	Book book(String isbn);

	@Find
	Optional<Book> bookMaybe(@By("id(this)") String id);

	@Find
	Book[] books(@By("isbn") String[] isbns);

	@Find
	List<Book> booksWithPages(List<Integer> pages);

	@Find
	List<Book> booksWithPages(int pages);

	@Find
	Optional<Book> bookIfAny(String isbn);

	@Find @Nullable
	Book bookOrNullIfNone(String isbn);

	@Find
	Author author(String ssn);

	@Find
	List<Author> authors(@By("ssn") String[] ssns);

	@Find
	Book byTitleAndDate(String title, LocalDate publicationDate);

	@Find
	Optional<Book> maybeByTitleAndDate(String title, LocalDate publicationDate);

	@Find
	Book bookById(@By("isbn") String id);

	@Find
	@OrderBy(value = "title", ignoreCase = true)
	List<Book> byPubDate0(LocalDate publicationDate);

	@Find
	@OrderBy(value = "title", ignoreCase = true)
	@OrderBy(value = "isbn", descending = true)
	List<Book> byPubDate1(LocalDate publicationDate, Limit limit, Sort<? super Book> order);

	@Find
	List<Book> byPubDate2(LocalDate publicationDate, Order<? super Book> order);

	@Find
	List<Book> byPubDate3(LocalDate publicationDate, Sort<? super Book>... order);

	@Find
	Stream<Book> byPubDate4(LocalDate publicationDate, Sort<? super Book> order);

	@Find
	Book[] bookArrayByTitle(String title);

	@Insert
	void create(Book book);

	@Update
	void update(Book book);

	@Delete
	void delete(Book book);

	@Save
	void createOrUpdate(Book book);

	@Query("from Book where title = :title")
	Book bookWithTitle(String title);

	@Nullable
	@Query("from Book where title = :title")
	Book bookWithTitleOrNullIfNone(String title);

	@Query("from Book where title = :title")
	Optional<Book> bookWithTitleMaybe(String title);

	@Query("from Book where title like :title")
	List<Book> books0(String title);

	@Query("from Book where title like :title")
	List<Book> books1(@Param("title") String titlePattern, Order<Book> order);

	@Query("from Book where title like :title")
	List<Book> books2(@Param("title") String titlePattern, Limit limit);

	@Query("from Book where title like :title")
	List<Book> books3(String title, Limit limit, Sort<Book>... order);

	@Query("from Book where title like :title")
	Book[] books4(String title, Sort<Book> sort);

	@Query("select title from Book where title like :title order by isbn")
	Stream<String> titles0(String title);

	@Query("select title from Book where title like :title order by isbn")
	String[] titles1(String title);

	@Query("from Book")
	Stream<Book> everyBook0(Order<? super Book> order);

	@Query("from Book")
	List<Book> everyBook1(PageRequest pageRequest);

	@Find
	List<Book> everyBook2(PageRequest pageRequest, Order<Book> order);

	@Query("from Book")
	@OrderBy("isbn")
	@OrderBy(value = "publicationDate", descending = true)
	List<Book> everyBook3(PageRequest pageRequest);

	@Find
	CursoredPage<Book> everyBook4(PageRequest pageRequest, Order<Book> order);

	@Find
	CursoredPage<Book> everyBook5(String title, PageRequest pageRequest, Order<Book> order);

	@Query("from Book")
	CursoredPage<Book> everyBook6(PageRequest pageRequest, Order<Book> order);

	@Query("from Book where title like :titlePattern")
	CursoredPage<Book> everyBook7(String titlePattern, PageRequest pageRequest, Order<Book> order);

	@Find
	CursoredPage<Book> everyBook8(String title, PageRequest pageRequest, Order<Book> order);

	@Query("from Book where title like :titlePattern")
	CursoredPage<Book> everyBook9(String titlePattern, PageRequest pageRequest, Order<Book> order);

	@Find
	Page<Book> booksByTitle1(String title, PageRequest pageRequest, Order<Book> order);

	@Query("from Book where title like :titlePattern")
	Page<Book> booksByTitle2(String titlePattern, PageRequest pageRequest, Order<Book> order);

	@Find
	List<Book> allBooksWithLotsOfSorting(Sort<? super Book> s1, Order<? super Book> order, Sort<? super Book>... s3);

	@Query("where price < ?1 and pages > ?2")
	Book[] valueBooks0(BigDecimal maxPrice, int minPages);

	@Query("where price < :maxPrice and pages > :minPages")
	Book[] valueBooks1(BigDecimal maxPrice, int minPages);

	@Query("where price < :price and pages > :pages")
	Book[] valueBooks2(@Param("price") BigDecimal maxPrice, @Param("pages") int minPages);

	@Save
	Book write(Book book);

	@Update
	Book edit(Book book);

	@Find
	List<Author> withNoOrder1(PageRequest pageRequest);

	@Query("")
	List<Author> withNoOrder2(PageRequest pageRequest);

	@Query("update Author set name = :name where ssn = :id")
	void updateAuthorAddress1(String id, String name);

	@Query("update Author set name = :name where ssn = :id")
	int updateAuthorAddress2(String id, String name);

	@Query("update Author set name = :name where ssn = :id")
	boolean updateAuthorAddress3(String id, String name);
}
