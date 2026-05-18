/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.stateful;

import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;

@Repository
public interface StatelessBookRepository {
	@Save
	void save(StatefulBook book);
}
