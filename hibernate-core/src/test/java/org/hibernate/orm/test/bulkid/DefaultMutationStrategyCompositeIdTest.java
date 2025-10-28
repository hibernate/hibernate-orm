/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bulkid;

import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;

/**
 * @author Vlad Mihalcea
 */
public class DefaultMutationStrategyCompositeIdTest extends AbstractMutationStrategyCompositeIdTest {

	@Override
	protected Class<? extends SqmMultiTableMutationStrategy> getMultiTableMutationStrategyClass() {
		return null;
	}

	@Override
	protected Class<? extends SqmMultiTableInsertStrategy> getMultiTableInsertStrategyClass() {
		return null;
	}
}
