/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.repository;

import jakarta.data.Order;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;
import org.hibernate.processor.test.integ.model.Product;

import java.util.List;
import java.util.stream.Stream;

@Repository
public interface OrderedCatalog extends BasicRepository<Product, Long> {

	List<Product> findByPriceGreaterThanEqualOrderByNameAsc(Double price);

	List<Product> findByPriceGreaterThanEqualOrderByIdAsc(Double price);

	Stream<Product> findByPriceGreaterThanEqual(Double price);

	List<Product> findByPriceGreaterThanEqualOrderByNameWithSorts(Double price, Order<Product> sorts);

	Page<Product> findByNameOrderById(String name, PageRequest pageRequest);

	long countByPriceGreaterThanEqual(Double price);
}
