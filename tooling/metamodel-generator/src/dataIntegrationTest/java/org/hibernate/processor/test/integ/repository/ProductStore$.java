/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.repository;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository
interface ProductStore$ extends ProductStore {

	@Override
	@Query("select count(this) from Product where price < :maxPrice")
	long cheaperThan(double maxPrice);
}
