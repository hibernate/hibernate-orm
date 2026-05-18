/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.stateful;

import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import jakarta.data.repository.stateful.Detach;
import jakarta.data.repository.stateful.Merge;
import jakarta.data.repository.stateful.Persist;
import jakarta.data.repository.stateful.Refresh;
import jakarta.data.repository.stateful.Remove;
import jakarta.persistence.EntityManager;

@Repository
public interface EntityManagerStatefulBookRepository {
	EntityManager entityManager();

	@Find
	StatefulBook byTitle(String title);

	@Persist
	void persist(StatefulBook book);

	@Merge
	StatefulBook merge(StatefulBook book);

	@Refresh
	void refresh(StatefulBook book);

	@Remove
	void remove(StatefulBook book);

	@Detach
	void detach(StatefulBook book);
}
