/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bulkid;

import org.hibernate.query.sqm.mutation.internal.inline.InlineMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;

/**
 * @author Vlad Mihalcea
 */
public class InlineMutationStrategyIdTest extends AbstractMutationStrategyIdTest {

	@Override
	protected Class<? extends SqmMultiTableMutationStrategy> getMultiTableMutationStrategyClass() {
		return InlineMutationStrategy.class;
	}

	@Override
	protected Class<? extends SqmMultiTableInsertStrategy> getMultiTableInsertStrategyClass() {
		// No inline strategy for insert
		return null;
	}
}
