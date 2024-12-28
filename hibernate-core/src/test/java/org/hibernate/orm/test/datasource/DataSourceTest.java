/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.datasource;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.internal.log.ConnectionInfoLogger;
import org.hibernate.testing.logger.LogInspectionHelper;
import org.hibernate.testing.logger.LogListener;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.hibernate.internal.util.StringHelper.split;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(annotatedClasses = DataSourceTest.TestEntity.class,
		integrationSettings = @Setting(name = JdbcSettings.CONNECTION_PROVIDER,
				value = "org.hibernate.orm.test.datasource.TestDataSourceConnectionProvider"))
public class DataSourceTest {
	@Test
	void test(EntityManagerFactoryScope scope) {
		Listener listener = new Listener();
		LogInspectionHelper.registerListener( listener, ConnectionInfoLogger.INSTANCE );
		scope.getEntityManagerFactory();
		LogInspectionHelper.clearAllListeners( ConnectionInfoLogger.INSTANCE );
		assertTrue( listener.seen );
	}

	@Entity(name="TestEntity")
	static class TestEntity {
		@Id
		long id;
	}

	private static class Listener implements LogListener {
		boolean seen = false;

		@Override
		public void loggedEvent(Logger.Level level, String renderedMessage, Throwable thrown) {
			if ( renderedMessage.contains( "Database info:" ) ) {
				seen = true;
				final Properties properties = Environment.getProperties();
				assertTrue( renderedMessage.contains( split( ";", properties.getProperty(JdbcSettings.URL) )[0] ) );
			}
		}
	}
}
