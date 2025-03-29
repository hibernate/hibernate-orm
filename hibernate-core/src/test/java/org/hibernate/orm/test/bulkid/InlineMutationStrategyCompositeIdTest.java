/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bulkid;

import org.hibernate.query.sqm.mutation.internal.inline.InlineMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;

/**
 * @author Vlad Mihalcea
 */
public class InlineMutationStrategyCompositeIdTest extends AbstractMutationStrategyCompositeIdTest {

	@Override
	protected Class<? extends SqmMultiTableMutationStrategy> getMultiTableBulkIdStrategyClass() {
		return InlineMutationStrategy.class;
	}
}
