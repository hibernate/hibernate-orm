package org.hibernate.orm.test.query.named;

import java.util.List;

import io.smallrye.mutiny.Uni;
import jakarta.persistence.query.JakartaQuery;

/**
 * Static queries without an explicit {@code from} clause, whose result
 * type is wrapped in a reactive type.
 */
public interface ReactiveRepository {
	@JakartaQuery( "where title = :title" )
	Uni<Book> uniByTitle(String title);

	@JakartaQuery( "where title = :title" )
	Uni<List<Book>> uniListByTitle(String title);
}
