/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bulkid;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.sqm.mutation.internal.inline.InlineMutationStrategy;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SettingProvider;

/**
 * @author Vlad Mihalcea
 */
@ServiceRegistry(
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.QUERY_MULTI_TABLE_MUTATION_STRATEGY,
						provider = InlineMutationStrategyIdTest.QueryMultyTableMutationStrategyProvider.class
				)
		}
)
public class InlineMutationStrategyIdTest extends AbstractMutationStrategyIdTest {

	public static class QueryMultyTableMutationStrategyProvider
			implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return InlineMutationStrategy.class.getName();
		}
	}
}
