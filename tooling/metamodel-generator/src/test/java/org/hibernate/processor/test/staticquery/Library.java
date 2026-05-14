/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.staticquery;

import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.QueryHint;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.query.JakartaQuery;
import jakarta.persistence.query.NativeQuery;
import jakarta.persistence.query.QueryOptions;

import java.util.List;

public abstract class Library {
	@JakartaQuery("from Book where title like :title")
	@QueryOptions(
			cacheStoreMode = CacheStoreMode.BYPASS,
			flush = QueryFlushMode.FLUSH,
			timeout = 500,
			entityGraph = "Book.summary",
			hints = @QueryHint(name = "org.hibernate.readOnly", value = "true")
	)
	abstract List<Book> findBooks(String title);

	@NativeQuery("select * from Book where isbn = ?")
	abstract Book nativeBook(String isbn);

	@JakartaQuery("delete from Book where obsolete = true")
	abstract int deleteObsolete();
}
