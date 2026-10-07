package org.hibernate.processor.test.nativequery;

import jakarta.persistence.query.NativeQuery;

import java.util.List;

public interface NativeQueries {

	record BookRecord(Long id, String title) {}

	@NativeQuery("select id, title from Book where title = :title")
	List<BookRecord> findByTitle(String title);
}
