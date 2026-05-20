/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.eg;

import org.hibernate.StatelessSession;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.query.QueryOptions;
import java.util.List;

@Repository
public interface BookshopWithDefault extends CrudRepository<Book, String> {
	StatelessSession session();

	@Query("select b from Book b join b.authors a where a.name = :authorName")
	@QueryOptions(cacheStoreMode = CacheStoreMode.BYPASS)
	default List<Book> booksBy(String authorName) {
		return session().createQuery( BookshopWithDefault_.booksBy( authorName ) )
				.setCacheable( true )
				.setCacheRegion( "books-by-author-name" )
				.setComment( "Books by Author name" )
				.getResultList();
	}
}
