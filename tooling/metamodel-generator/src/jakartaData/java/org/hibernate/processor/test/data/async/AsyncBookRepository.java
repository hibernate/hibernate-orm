/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.async;

import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.enterprise.concurrent.Asynchronous;

@Repository
public interface AsyncBookRepository {
	@Asynchronous
	@Find
	CompletionStage<AsyncBook> bookByIsbn(String isbn);

	@Asynchronous
	@Find
	CompletionStage<List<AsyncBook>> booksByTitle(String title);

	@Asynchronous
	@Query("where title = :title")
	CompletionStage<List<AsyncBook>> booksWithQuery(String title);

	@Asynchronous
	@Query("update AsyncBook set title = :title where isbn = :isbn")
	CompletionStage<Integer> updateTitle(@Param("isbn") String isbn, @Param("title") String title);

	@Asynchronous
	@Delete
	CompletionStage<Long> deleteByTitle(String title);

	@Asynchronous
	@Insert
	CompletionStage<Void> insertBook(AsyncBook book);
}
