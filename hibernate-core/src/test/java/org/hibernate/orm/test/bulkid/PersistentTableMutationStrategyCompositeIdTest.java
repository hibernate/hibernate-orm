/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bulkid;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableMutationStrategy;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SettingProvider;

@ServiceRegistry(
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.QUERY_MULTI_TABLE_MUTATION_STRATEGY,
						provider = PersistentTableMutationStrategyCompositeIdTest.QueryMultyTableMutationStrategyProvider.class
				),
				@SettingProvider(
						settingName = AvailableSettings.QUERY_MULTI_TABLE_INSERT_STRATEGY,
						provider = PersistentTableMutationStrategyCompositeIdTest.QueryMultyTableInsertStrategyProvider.class
				)
		}
)
public class PersistentTableMutationStrategyCompositeIdTest extends AbstractMutationStrategyCompositeIdTest {

	public static class QueryMultyTableMutationStrategyProvider
			implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return PersistentTableMutationStrategy.class.getName();
		}
	}

	public static class QueryMultyTableInsertStrategyProvider
			implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return PersistentTableInsertStrategy.class.getName();
		}
	}
}
