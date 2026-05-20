/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.named;

import jakarta.data.repository.Repository;

@Repository
public interface BookRepository
		extends BookRepositoryQueries,
				GenericBookRepository<Book> {
}
