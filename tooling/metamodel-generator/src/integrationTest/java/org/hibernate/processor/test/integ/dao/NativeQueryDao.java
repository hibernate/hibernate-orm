package org.hibernate.processor.test.integ.dao;

import jakarta.persistence.query.NativeQuery;
import org.hibernate.Session;
import org.hibernate.annotations.processing.SQL;

import java.util.List;

public interface NativeQueryDao {

	Session session();

	record BookRecord(String isbn, String title) {}

	@NativeQuery("select isbn, title from integ_books where title = :title order by isbn")
	List<BookRecord> findByTitle(String title);

	@SQL("select isbn, title from integ_books where title = :title order by isbn")
	List<BookRecord> findByTitleSql(String title);
}
