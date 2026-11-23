package org.hibernate.orm.test.query.named;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import jakarta.persistence.query.JakartaQuery;
import org.hibernate.annotations.processing.Exclude;

/**
 * Static queries without an explicit {@code from} clause, whose result
 * type is wrapped in {@code CompletionStage}.
 * <p>
 * Excluded from annotation processing, which only accepts {@code CompletionStage}
 * with {@code @Asynchronous}, not available here. See {@code AsyncBookRepositoryTest}
 * in the processor for the same thing with {@code @Asynchronous}.
 */
@Exclude
public interface AsynchronousRepository {
	@JakartaQuery( "where title = :title" )
	CompletionStage<Book> stageByTitle(String title);

	@JakartaQuery( "where title = :title" )
	CompletionStage<List<Book>> stageListByTitle(String title);

	@JakartaQuery( "where title = :title" )
	CompletionStage<Optional<Book>> stageOptionalByTitle(String title);
}
