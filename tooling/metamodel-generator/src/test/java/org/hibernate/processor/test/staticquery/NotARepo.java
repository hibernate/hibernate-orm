/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.staticquery;

import jakarta.persistence.query.JakartaQuery;

import java.util.List;

public interface NotARepo {
	record Summary(String isbn, String title) {
	}

	@JakartaQuery("from Book")
	List<Book> books();

	@JakartaQuery("from Book")
	List<Summary> summaries();
}
