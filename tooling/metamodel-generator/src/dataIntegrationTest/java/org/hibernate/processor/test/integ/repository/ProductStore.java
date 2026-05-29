/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.repository;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import org.hibernate.processor.test.integ.model.Product;

import java.util.List;

@Repository
public interface ProductStore extends DataRepository<Product, Long> {

	@Save
	void save(Product product);

	@Query("select count(this)")
	long countAll();

	@Query("select this where price > :minPrice order by name")
	List<Product> expensiveThan(double minPrice);

	@Query("select this.name where this.price > :minPrice order by this.name")
	List<String> expensiveNames(double minPrice);

	long cheaperThan(double maxPrice);
}
