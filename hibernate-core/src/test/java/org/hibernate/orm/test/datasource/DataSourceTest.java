/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.datasource;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.internal.log.ConnectionInfoLogger;
import org.hibernate.testing.logger.LogInspectionHelper;
import org.hibernate.testing.logger.LogListener;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;


import static org.hibernate.internal.util.StringHelper.split;
import static org.hibernate.testing.jdbc.GradleParallelTestingResolver.resolveUrl;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(annotatedClasses = DataSourceTest.TestEntity.class,
		integrationSettings = @Setting(name = JdbcSettings.CONNECTION_PROVIDER,
				value = "org.hibernate.orm.test.datasource.TestDataSourceConnectionProvider"))
public class DataSourceTest {
	@Test
	void test(EntityManagerFactoryScope scope) {
		Listener listener = new Listener();
		LogInspectionHelper.registerListener( listener, ConnectionInfoLogger.CONNECTION_INFO_LOGGER );
		scope.getEntityManagerFactory();
		LogInspectionHelper.clearAllListeners( ConnectionInfoLogger.CONNECTION_INFO_LOGGER );
		Dialect dialect = scope.getDialect();
		assertTrue( dialect instanceof OracleDialect
					|| dialect instanceof DB2Dialect
					|| dialect instanceof InformixDialect // Informix metadata does not include the URL
					|| listener.seen );
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
				final String url = resolveUrl( Environment.getProperties().getProperty( JdbcSettings.URL ) );
				final String firstUrlPart = split( "?", split( ";", url )[0])[0];
				final String baseUrl = firstUrlPart.endsWith( "/" ) ? firstUrlPart.substring( 0, firstUrlPart.length() - 1 ) : firstUrlPart;
				seen = renderedMessage.contains( baseUrl );
			}
		}
	}
}
