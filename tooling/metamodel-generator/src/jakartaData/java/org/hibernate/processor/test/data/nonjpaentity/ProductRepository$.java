/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.nonjpaentity;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
interface ProductRepository$ extends ProductRepository {

	@Override
	@Query("delete from Product where id = ?1")
	void deleteById(Long id);

	@Override
	@Query("where name = ?1")
	List<Product> findByName(String name);
}
