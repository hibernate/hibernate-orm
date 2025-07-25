/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bulkid;

import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;

@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsGlobalTemporaryTable.class)
public class GlobalTemporaryTableMutationStrategyIdTest extends AbstractMutationStrategyIdTest {

	@Override
	protected Class<? extends SqmMultiTableMutationStrategy> getMultiTableMutationStrategyClass() {
		return GlobalTemporaryTableMutationStrategy.class;
	}

	@Override
	protected Class<? extends SqmMultiTableInsertStrategy> getMultiTableInsertStrategyClass() {
		return GlobalTemporaryTableInsertStrategy.class;
	}
}
