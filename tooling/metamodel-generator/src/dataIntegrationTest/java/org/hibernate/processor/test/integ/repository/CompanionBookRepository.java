package org.hibernate.processor.test.integ.repository;

import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;
import org.hibernate.StatelessSession;
import org.hibernate.processor.test.integ.model.Book;

import java.util.List;

@Repository
public interface CompanionBookRepository {

	StatelessSession session();

	@Insert
	void insert(Book book);

	@Find
	Book byIsbn(String isbn);

	@Find
	List<Book> byTitle(String title);

	long countAll();
}
