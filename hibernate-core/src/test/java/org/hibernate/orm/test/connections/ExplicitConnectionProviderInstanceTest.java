/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.connections;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class ExplicitConnectionProviderInstanceTest extends BaseUnitTestCase {
	@Test
	public void testPassingConnectionProviderInstanceToBootstrap() {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.CONNECTION_PROVIDER, TestingConnectionProviderImpl.INSTANCE )
				.build();
		try {
			assert ssr.getService( ConnectionProvider.class ) == TestingConnectionProviderImpl.INSTANCE;
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	public static class TestingConnectionProviderImpl implements ConnectionProvider {
		/**
		 * Singleton access
		 */
		public static final TestingConnectionProviderImpl INSTANCE = new TestingConnectionProviderImpl();

		@Override
		public Connection getConnection() throws SQLException {
			return null;
		}

		@Override
		public void closeConnection(Connection connection) {
		}

		@Override
		public boolean supportsAggressiveRelease() {
			return false;
		}

		@Override
		public boolean isUnwrappableAs(Class<?> unwrapType) {
			return false;
		}

		@Override
		public <T> T unwrap(Class<T> unwrapType) {
			return null;
		}
	}
}
