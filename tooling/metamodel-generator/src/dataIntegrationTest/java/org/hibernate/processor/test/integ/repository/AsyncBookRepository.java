package org.hibernate.processor.test.integ.repository;

import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.enterprise.concurrent.Asynchronous;
import org.hibernate.StatelessSession;
import org.hibernate.processor.test.integ.model.Book;

/**
 * Queries without a {@code from} clause, with a result wrapped in {@code CompletionStage}.
 */
@Repository
public interface AsyncBookRepository {

	StatelessSession session();

	@Asynchronous
	@Query("where title = :title")
	CompletionStage<Book> asyncByTitle(String title);

	@Asynchronous
	@Query("where title = :title order by isbn")
	CompletionStage<List<Book>> asyncListByTitle(String title);
}
