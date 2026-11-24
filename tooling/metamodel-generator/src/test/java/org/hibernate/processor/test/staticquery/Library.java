package org.hibernate.processor.test.staticquery;

import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.QueryHint;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.query.JakartaQuery;
import jakarta.persistence.query.JakartaStatement;
import jakarta.persistence.query.NativeQuery;
import jakarta.persistence.query.NativeStatement;
import jakarta.persistence.query.QueryOptions;
import jakarta.persistence.query.StatementOptions;

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

	@JakartaStatement("update Book set obsolete = true where isbn = :isbn")
	@StatementOptions(
			flush = QueryFlushMode.NO_FLUSH,
			timeout = 750,
			hints = @QueryHint(name = "org.hibernate.comment", value = "retire")
	)
	abstract int retire(String isbn);

	@NativeStatement("/* not recognizable by its first keyword */ delete from Book where obsolete = true")
	abstract void purge();
}
