/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.eg;

import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.stream.Stream;

@Repository
public interface Publishers extends BasicRepository<Publisher,Long> {
	@Query(" ")
	Stream<Publisher> all();

	@Find
	Publisher find(Long id);
}
