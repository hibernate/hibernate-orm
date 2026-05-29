/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.nonjpaentity;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;

import java.util.List;

@Repository
public interface ProductRepository extends DataRepository<Product, Long> {

	@Save
	void save(Product product);

	void deleteById(Long id);

	List<Product> findByName(String name);
}
