/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.cursoredpage;

import jakarta.data.page.CursoredPage;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;
import org.hibernate.processor.test.data.namedquery.Book;

@Repository
public interface CursoredBookRepository {
	@Find
	@OrderBy("title")
	CursoredPage<Book> allBooks(PageRequest pageRequest);
}
