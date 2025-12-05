/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bulkid;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableInsertStrategy;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SettingProvider;

@ServiceRegistry(
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.QUERY_MULTI_TABLE_INSERT_STRATEGY,
						provider = PersistentTableMutationStrategyGeneratedIdTest.QueryMultyTableInsertStrategyProvider.class
				)
		}
)
public class PersistentTableMutationStrategyGeneratedIdTest extends AbstractMutationStrategyGeneratedIdTest {

	public static class QueryMultyTableInsertStrategyProvider
			implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return PersistentTableInsertStrategy.class.getName();
		}
	}
}
