/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.repository;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository
interface CompanionBookRepository$ extends CompanionBookRepository {

	@Override
	@Query("select count(*) from Book")
	long countAll();
}
