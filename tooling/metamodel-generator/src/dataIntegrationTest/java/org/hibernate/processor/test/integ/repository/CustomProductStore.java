/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.repository;

import jakarta.data.repository.Insert;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import org.hibernate.StatelessSession;
import org.hibernate.processor.test.integ.model.Product;

import java.util.List;

/**
 * Repository without built-in supertypes. The primary entity class
 * is inferred from the lifecycle method parameter types.
 */
@Repository
public interface CustomProductStore {

	StatelessSession session();

	@Insert
	void add(List<Product> products);

	@Delete
	void remove(List<Product> products);

	@Query("select count(this)")
	long countAll();
}
