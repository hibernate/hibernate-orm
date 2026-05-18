/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.stateful;

import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.stateful.Merge;
import jakarta.data.repository.stateful.Persist;
import org.hibernate.StatelessSession;

@Repository
public interface InvalidStatefulBookRepository {
	StatelessSession session();

	@Persist
	void persist(StatefulBook book);

	@Persist
	StatefulBook persistAndReturn(StatefulBook book);

	@Merge
	void merge(StatefulBook book);

	@Save
	void save(StatefulBook book);
}
