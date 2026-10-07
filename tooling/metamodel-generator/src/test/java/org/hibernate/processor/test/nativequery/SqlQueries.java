package org.hibernate.processor.test.nativequery;

import org.hibernate.annotations.processing.SQL;

import java.util.List;

public interface SqlQueries {

	@SQL("select id, title from Book where title = :title")
	List<NativeQueries.BookRecord> findByTitle(String title);
}
