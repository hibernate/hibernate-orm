/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bulkid;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SettingProvider;

@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsLocalTemporaryTable.class)
@ServiceRegistry(
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.QUERY_MULTI_TABLE_MUTATION_STRATEGY,
						provider = LocalTemporaryTableMutationStrategyCompositeIdTest.QueryMultyTableMutationStrategyProvider.class
				),
				@SettingProvider(
						settingName = AvailableSettings.QUERY_MULTI_TABLE_INSERT_STRATEGY,
						provider = LocalTemporaryTableMutationStrategyCompositeIdTest.QueryMultyTableInsertStrategyProvider.class
				)
		}
)
public class LocalTemporaryTableMutationStrategyCompositeIdTest extends AbstractMutationStrategyCompositeIdTest {

	public static class QueryMultyTableMutationStrategyProvider
			implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return LocalTemporaryTableMutationStrategy.class.getName();
		}
	}

	public static class QueryMultyTableInsertStrategyProvider
			implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return LocalTemporaryTableInsertStrategy.class.getName();
		}
	}
}
