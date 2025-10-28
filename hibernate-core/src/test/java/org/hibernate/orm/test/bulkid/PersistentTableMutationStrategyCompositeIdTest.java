/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bulkid;

import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;

public class PersistentTableMutationStrategyCompositeIdTest extends AbstractMutationStrategyCompositeIdTest {

	@Override
	protected Class<? extends SqmMultiTableMutationStrategy> getMultiTableMutationStrategyClass() {
		return PersistentTableMutationStrategy.class;
	}

	@Override
	protected Class<? extends SqmMultiTableInsertStrategy> getMultiTableInsertStrategyClass() {
		return PersistentTableInsertStrategy.class;
	}
}
