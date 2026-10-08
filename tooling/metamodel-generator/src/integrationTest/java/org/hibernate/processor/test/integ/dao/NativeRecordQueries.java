package org.hibernate.processor.test.integ.dao;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.query.NativeQuery;

import java.util.List;

@NamedNativeQuery(name = "booksAsRecords",
		query = "select isbn, title from integ_books where title = ?1 order by isbn",
		resultClass = NativeRecordQueries.BookRecord.class)
public interface NativeRecordQueries {

	record BookRecord(String isbn, String title) {}

	@NativeQuery("select isbn, title from integ_books where title = ?1 order by isbn")
	List<BookRecord> findByTitle(String title);

	@NativeQuery("select isbn, title from integ_books where title = ?1 order by isbn")
	@ConstructorResult(targetClass = BookRecord.class, columns = {
			@ColumnResult(name = "isbn", type = String.class),
			@ColumnResult(name = "title", type = String.class)
	})
	List<BookRecord> findByTitleMapped(String title);
}
