/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.hhh20212;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import org.hibernate.StatelessSession;

@Repository
public interface HHH20212Repository extends CrudRepository<HHH20212Entity, Long> {
	default StatelessSession getStatelessSession() {
		return null;
	}

	@Find
	HHH20212Entity byId(Long id);
}
