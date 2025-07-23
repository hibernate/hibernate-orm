/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bulkid;

import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;

public class DefaultMutationStrategyGeneratedIdWithOptimizerTest extends AbstractMutationStrategyGeneratedIdWithOptimizerTest {

	@Override
	protected Class<? extends SqmMultiTableInsertStrategy> getMultiTableInsertStrategyClass() {
		return null;
	}
}
