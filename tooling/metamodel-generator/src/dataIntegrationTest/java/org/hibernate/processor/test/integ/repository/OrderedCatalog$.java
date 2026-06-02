/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.repository;

import static jakarta.data.repository.By.ID;

import jakarta.data.Order;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import org.hibernate.processor.test.integ.model.Product;

import java.util.List;
import java.util.stream.Stream;

@Repository
interface OrderedCatalog$ extends OrderedCatalog {

	@Override
	@Find
	@OrderBy("name")
	List<Product> findByPriceGreaterThanEqualOrderByNameAsc(Double price);

	@Override
	@Find
	@OrderBy(ID)
	List<Product> findByPriceGreaterThanEqualOrderByIdAsc(Double price);

	@Override
	@Query("where price >= ?1")
	@OrderBy(ID)
	Stream<Product> findByPriceGreaterThanEqual(Double price);

	@Override
	@Query("where price >= ?1")
	@OrderBy(value = "name", descending = false)
	List<Product> findByPriceGreaterThanEqualOrderByNameWithSorts(Double price, Order<Product> sorts);

	@Override
	@Find
	@OrderBy(ID)
	Page<Product> findByNameOrderById(String name, PageRequest pageRequest);

	@Override
	@Query("select count(*) from Product where price >= ?1")
	long countByPriceGreaterThanEqual(Double price);
}
