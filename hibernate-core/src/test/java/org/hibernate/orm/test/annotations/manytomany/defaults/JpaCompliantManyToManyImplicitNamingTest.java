/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytomany.defaults;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

/**
 * Tests names generated for @JoinTable and @JoinColumn for unidirectional and bidirectional
 * many-to-many associations using the JPA-compliant naming strategy.
 *
 * @author Gail Badner
 */
@ServiceRegistry(
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.IMPLICIT_NAMING_STRATEGY,
						provider = JpaCompliantManyToManyImplicitNamingTest.ImplicitNamingStrategyProvider.class
				)
		}
)
public class JpaCompliantManyToManyImplicitNamingTest extends ManyToManyImplicitNamingTest {

	public static class ImplicitNamingStrategyProvider implements SettingProvider.Provider<ImplicitNamingStrategy> {
		@Override
		public ImplicitNamingStrategy getSetting() {
			return ImplicitNamingStrategyJpaCompliantImpl.INSTANCE;
		}
	}

	@Test
	@JiraKey(value = "HHH-9390")
	public void testUnidirOwnerPrimaryTableAssocEntityNamePKOverride(SessionFactoryScope scope) {
		// City.stolenItems; associated entity: Item
		// City has @Entity with no name configured and @Table(name = "tbl_city")
		// Item has @Entity(name="ITEM") and no @Table
		// PK column for City.id: id (default)
		// PK column for Item: iId
		// unidirectional
		checkDefaultJoinTablAndJoinColumnNames(
				City.class,
				"stolenItems",
				null,
				"tbl_city_ITEM",
				"City_id",
				"stolenItems_iId",
				scope
		);
	}

	@Test
	@JiraKey(value = "HHH-9390")
	public void testUnidirOwnerEntityNamePrimaryTableOverride(SessionFactoryScope scope) {
		// Category.clients: associated entity: KnownClient
		// Category has @Entity(name="CATEGORY") @Table(name="CATEGORY_TAB")
		// KnownClient has @Entity with no name configured and no @Table
		// PK column for Category.id: id (default)
		// PK column for KnownClient.id: id (default)
		// unidirectional
		checkDefaultJoinTablAndJoinColumnNames(
				Category.class,
				"clients",
				null,
				"CATEGORY_TAB_KnownClient",
				"CATEGORY_id",
				"clients_id",
				scope
		);
	}
}
