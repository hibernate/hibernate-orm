/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.inheritance;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.sqm.mutation.internal.inline.InlineMutationStrategy;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SettingProvider;

/**
 * Runs the same {@link SoftDeleteTablePerClassTest} scenarios using
 * {@link InlineMutationStrategy} instead of the default mutation strategy.
 */
@DomainModel(annotatedClasses = {
		SoftDeleteTablePerClassTest.Book.class,
		SoftDeleteTablePerClassTest.SpellBook.class,
		SoftDeleteTablePerClassTest.DarkSpellBook.class,
		SoftDeleteTablePerClassTest.Novel.class
})
@SessionFactory
@ServiceRegistry(settingProviders = {
		@SettingProvider(
				settingName = AvailableSettings.QUERY_MULTI_TABLE_MUTATION_STRATEGY,
				provider = SoftDeleteTablePerClassInlineTest.InlineMutationStrategyProvider.class
		)
})
public class SoftDeleteTablePerClassInlineTest extends SoftDeleteTablePerClassTest {

	public static class InlineMutationStrategyProvider
			implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return InlineMutationStrategy.class.getName();
		}
	}
}
