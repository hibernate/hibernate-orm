/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.repository;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import org.hibernate.processor.test.integ.model.Product;

import java.util.List;

@Repository
public interface Catalog extends DataRepository<Product, Long> {

	@Save
	void save(Product product);

	void deleteById(Long id);

	long countByPriceGreaterThanEqual(Double price);

	List<Product> findByName(String name);
}
