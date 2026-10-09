package org.hibernate.processor.test.data.entityprojection;

import java.util.List;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository
public interface Library {
	// Entity classes are being compiled along with this record projection.
	record BookWithAuthor(Book book, Author author) {}

	@Query("select b, a from Book b join b.authors a order by b.isbn, a.ssn")
	List<BookWithAuthor> booksWithAuthors();
}
