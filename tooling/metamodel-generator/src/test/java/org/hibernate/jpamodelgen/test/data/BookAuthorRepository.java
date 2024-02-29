package org.hibernate.jpamodelgen.test.data;

import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.PageRequest;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Repository(dataStore = "myds")
public interface BookAuthorRepository {

	StatelessSession session();

	@Insert
	void insertBooks0(Book[] books);

	@Insert
	void insertBooks1(Iterable<Book> books);

	@Insert
	void insertBooks2(Set<Book> books);

	@Find
	Book book(String isbn);

	@Find
	Optional<Book> bookIfAny(String isbn);

	@Find
	Author author(String ssn);

	@Find
	Book byTitleAndDate(String title, LocalDate publicationDate);

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

	@Query("from Book where title like :title")
	List<Book> books0(String title);

	@Query("from Book where title like :title")
	List<Book> books1(@Param("title") String titlePattern, Order<Book> order);

	@Query("from Book where title like :title")
	List<Book> books2(@Param("title") String titlePattern, Limit limit);

	@Query("from Book where title like :title")
	List<Book> books3(String title, Limit limit, Sort<Book>... order);

	@Query("select title from Book where title like :title order by isbn")
	Stream<String> titles(String title);

	@Query("from Book")
	List<Book> everyBook1(PageRequest<? super Book> pageRequest);

	@Find
	List<Book> everyBook2(PageRequest<? super Book> pageRequest);

	@Query("from Book")
	@OrderBy("isbn")
	@OrderBy(value = "publicationDate", descending = true)
	List<Book> everyBook3(PageRequest<? super Book> pageRequest);

	@Find
	KeysetAwareSlice<Book> everyBook4(PageRequest<Book> pageRequest);

	@Find
	KeysetAwareSlice<Book> everyBook5(String title, PageRequest<Book> pageRequest);

	@Query("from Book")
	KeysetAwareSlice<Book> everyBook6(PageRequest<Book> pageRequest);

	@Query("from Book where title like :titlePattern")
	KeysetAwareSlice<Book> everyBook7(String titlePattern, PageRequest<Book> pageRequest);

	@Find
	KeysetAwarePage<Book> everyBook8(String title, PageRequest<Book> pageRequest);

	@Query("from Book where title like :titlePattern")
	KeysetAwarePage<Book> everyBook9(String titlePattern, PageRequest<Book> pageRequest);

	@Find
	Page<Book> booksByTitle1(String title, PageRequest<Book> pageRequest);

	@Query("from Book where title like :titlePattern")
	Slice<Book> booksByTitle2(String titlePattern, PageRequest<Book> pageRequest);
}
