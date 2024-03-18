package org.hibernate.processor.test.data.eg;

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

import java.time.LocalDate;
import java.util.List;

@Repository
public interface Library {

	@Find
	Book book(String isbn);

	@Find
	List<Book> books(@By("isbn") List<String> isbns);

	@Find
	Book books(String title, LocalDate publicationDate);

	@Find
	@OrderBy("title")
	List<Book> booksByPublisher(String publisher_name);

	@Query("where title like :titlePattern")
	List<Book> booksByTitle(String titlePattern);

	// not required by Jakarta Data
	record BookWithAuthor(Book book, Author author) {}
	@Query("select b, a from Book b join b.authors a")
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

	@Find
	@OrderBy("isbn")
	CursoredPage<Book> allBooks(PageRequest<Book> pageRequest);

	@Find
	@OrderBy("name")
	@OrderBy("address.city") //not working currently
	List<Author> authors();

	@Find
	List<Author> authorsByCity(@By("address.city") String city);

	@Find
	List<Author> authorsByCityAndPostcode(String address_city, String address_postcode);
}
