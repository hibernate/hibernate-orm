/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.hqlvalidation;

import org.hibernate.annotations.processing.HQL;

import java.util.List;

interface BadDao {
	@HQL("select b from Book b join b.authors a where b.isb = ''")
	List<Book> booksWithAuthors();

}
