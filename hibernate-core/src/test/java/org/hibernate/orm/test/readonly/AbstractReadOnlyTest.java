/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.readonly;

import org.hibernate.CacheMode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Gail Badner
 */
@SessionFactory(
		generateStatistics = true
)
@ServiceRegistry(
		settings = @Setting(name = Environment.STATEMENT_BATCH_SIZE, value = "0"),
		settingProviders = @SettingProvider(settingName = AvailableSettings.JAKARTA_SHARED_CACHE_RETRIEVE_MODE, provider = AbstractReadOnlyTest.CacheModeProvider.class)
)
public abstract class AbstractReadOnlyTest {

	public static class CacheModeProvider implements SettingProvider.Provider<CacheMode> {

		@Override
		public CacheMode getSetting() {
			return CacheMode.IGNORE;
		}
	}

	protected void clearCounts(SessionFactoryScope scope) {
		scope.getSessionFactory().getStatistics().clear();
	}

	protected void assertInsertCount(int expected, SessionFactoryScope scope) {
		int inserts = (int) scope.getSessionFactory().getStatistics().getEntityInsertCount();
		assertEquals( expected, inserts, "unexpected insert count" );
	}

	protected void assertUpdateCount(int expected, SessionFactoryScope scope) {
		int updates = (int) scope.getSessionFactory().getStatistics().getEntityUpdateCount();
		assertEquals( expected, updates, "unexpected update counts" );
	}

	protected void assertDeleteCount(int expected, SessionFactoryScope scope) {
		int deletes = (int) scope.getSessionFactory().getStatistics().getEntityDeleteCount();
		assertEquals( expected, deletes, "unexpected delete counts" );
	}
}
