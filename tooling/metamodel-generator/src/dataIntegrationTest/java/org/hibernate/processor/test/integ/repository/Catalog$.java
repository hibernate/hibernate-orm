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
interface Catalog$ extends Catalog {

	@Override
	@Query("delete from Product where id = ?1")
	void deleteById(Long id);

	@Override
	@Query("select count(*) from Product where price >= ?1")
	long countByPriceGreaterThanEqual(Double price);

	@Override
	@Query("where name = ?1")
	List<Product> findByName(String name);
}
