/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.hhh20221;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;
import jakarta.persistence.TypedQuery;

@Repository
public interface HHH20221Repository extends CrudRepository<HHH20221Entity, Long> {
	@Find
	@OrderBy("id")
	TypedQuery<HHH20221Entity> findByIdOrdered(Long id);
}
