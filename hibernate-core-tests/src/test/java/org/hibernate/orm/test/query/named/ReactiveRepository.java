package org.hibernate.orm.test.query.named;

import java.util.List;

import io.smallrye.mutiny.Uni;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.persistence.query.JakartaQuery;
import org.hibernate.query.KeyedPage;
import org.hibernate.query.KeyedResultList;

/**
 * Static queries without an explicit {@code from} clause, whose result
 * type is wrapped in a reactive type.
 */
public interface ReactiveRepository {
	@JakartaQuery( "where title = :title" )
	Uni<Book> uniByTitle(String title);

	@JakartaQuery( "where title = :title" )
	Uni<List<Book>> uniListByTitle(String title);

	@JakartaQuery( "where title = :title" )
	Uni<Page<Book>> uniPageByTitle(String title, PageRequest pageRequest);

	@JakartaQuery( "select title from Jpa4StaticQueryBook where title = :title" )
	Uni<String[]> uniTitlesByTitle(String title);

	@JakartaQuery( "where title = :title order by id" )
	Uni<CursoredPage<Book>> uniCursoredPageByTitle(String title, PageRequest pageRequest);

	@JakartaQuery( "where title = :title order by id" )
	Uni<KeyedResultList<Book>> uniKeyedResultListByTitle(String title, KeyedPage<Book> page);
}
