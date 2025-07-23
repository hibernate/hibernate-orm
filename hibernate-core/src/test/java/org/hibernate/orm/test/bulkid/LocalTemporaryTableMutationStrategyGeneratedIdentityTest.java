/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bulkid;

import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;

@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsLocalTemporaryTable.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsLocalTemporaryTableIdentity.class)
public class LocalTemporaryTableMutationStrategyGeneratedIdentityTest extends AbstractMutationStrategyGeneratedIdentityTest {

	@Override
	protected Class<? extends SqmMultiTableInsertStrategy> getMultiTableInsertStrategyClass() {
		return LocalTemporaryTableInsertStrategy.class;
	}
}
