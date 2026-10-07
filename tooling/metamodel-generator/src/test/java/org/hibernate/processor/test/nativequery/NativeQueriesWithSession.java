package org.hibernate.processor.test.nativequery;

import jakarta.persistence.query.NativeQuery;
import org.hibernate.Session;

import java.util.List;

public interface NativeQueriesWithSession {

	Session session();

	@NativeQuery("select id, title from Book where title = :title")
	List<NativeQueries.BookRecord> findByTitle(String title);
}
