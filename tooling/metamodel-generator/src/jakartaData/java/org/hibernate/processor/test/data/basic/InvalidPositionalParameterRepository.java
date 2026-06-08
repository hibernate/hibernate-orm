/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.basic;

import jakarta.data.Limit;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
interface InvalidPositionalParameterRepository {
	@Query("from Book where isbn = ?2")
	List<Book> booksByIsbn(Limit limit, String isbn);

	@Query("from Book where isbn = ?1 and title = :title")
	List<Book> booksByIsbnAndTitle(String isbn, String title);
}
