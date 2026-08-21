/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.compliance;

import jakarta.data.Limit;
import jakarta.data.Sort;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;
import org.hibernate.StatelessSession;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

@Repository(dataStore = "myds")
public interface BookRepository {

	StatelessSession session();

	@Find
	@OrderBy(value = "title", ignoreCase = true)
	@OrderBy(value = "isbn", descending = true)
	List<Book> byPubDate1(LocalDate publicationDate, Limit limit, Sort<?> order);

	@Find
	List<Book> byPubDate3(LocalDate publicationDate, Sort<?>... order);

	@Find
	Stream<Book> byPubDate4(LocalDate publicationDate, Sort<?> order);

}
