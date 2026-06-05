/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.async;

import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import jakarta.enterprise.concurrent.Asynchronous;

@Repository
public interface InvalidAsyncBookRepository {
	@Asynchronous
	@Find
	AsyncBook bookByIsbn(String isbn);
}
