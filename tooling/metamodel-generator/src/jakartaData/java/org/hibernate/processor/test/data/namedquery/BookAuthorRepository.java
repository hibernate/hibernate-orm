/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.namedquery;

import jakarta.data.repository.Repository;
import org.hibernate.processor.test.data.namedquery.Book.Type;

import java.util.List;
import java.util.Set;

@Repository(dataStore = "myds")
public interface BookAuthorRepository {
	List<Book> findByTitleLike(String title);
	List<Book> findByTypeIn(Set<Type> types);
}
