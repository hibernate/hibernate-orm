/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.repository;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import org.hibernate.processor.test.integ.model.Product;

import java.util.List;

@Repository
interface MixedCatalog$ extends MixedCatalog {

	@Override
	@Query("where name = ?1")
	List<Product> findByName(String name);

	@Override
	@Query("select count(*) from Product where price >= ?1")
	long countByPriceGreaterThanEqual(Double price);
}
