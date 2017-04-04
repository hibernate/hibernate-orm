/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.common.connections;

import java.sql.Connection;
import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public abstract class BaseTransactionIsolationConfigTest extends BaseUnitTestCase {
	protected abstract ConnectionProvider getConnectionProviderUnderTest();

	protected void augmentConfigurationSettings(Properties properties) {
	}

	@Test
	public void testSettingIsolationAsNumeric() throws Exception {
		Properties properties = Environment.getProperties();
		augmentConfigurationSettings( properties );
		properties.put( AvailableSettings.ISOLATION, Connection.TRANSACTION_SERIALIZABLE );

		ConnectionProvider provider = getConnectionProviderUnderTest();

		try {
			( (Configurable) provider ).configure( properties );

			if ( Startable.class.isInstance( provider ) ) {
				( (Startable) provider ).start();
			}

			Connection connection = provider.getConnection();
			assertEquals( Connection.TRANSACTION_SERIALIZABLE, connection.getTransactionIsolation() );
			provider.closeConnection( connection );
		}
		finally {
			( (Stoppable) provider ).stop();
		}
	}

	@Test
	public void testSettingIsolationAsNumericString() throws Exception {
		Properties properties = Environment.getProperties();
		augmentConfigurationSettings( properties );
		properties.put( AvailableSettings.ISOLATION, Integer.toString( Connection.TRANSACTION_SERIALIZABLE ) );

		ConnectionProvider provider = getConnectionProviderUnderTest();

		try {
			( (Configurable) provider ).configure( properties );

			if ( Startable.class.isInstance( provider ) ) {
				( (Startable) provider ).start();
			}

			Connection connection = provider.getConnection();
			assertEquals( Connection.TRANSACTION_SERIALIZABLE, connection.getTransactionIsolation() );
			provider.closeConnection( connection );
		}
		finally {
			( (Stoppable) provider ).stop();
		}
	}

	@Test
	public void testSettingIsolationAsName() throws Exception {
		Properties properties = Environment.getProperties();
		augmentConfigurationSettings( properties );
		properties.put( AvailableSettings.ISOLATION, "TRANSACTION_SERIALIZABLE" );

		ConnectionProvider provider = getConnectionProviderUnderTest();

		try {
			( (Configurable) provider ).configure( properties );

			if ( Startable.class.isInstance( provider ) ) {
				( (Startable) provider ).start();
			}

			Connection connection = provider.getConnection();
			assertEquals( Connection.TRANSACTION_SERIALIZABLE, connection.getTransactionIsolation() );
			provider.closeConnection( connection );
		}
		finally {
			( (Stoppable) provider ).stop();
		}
	}

	@Test
	public void testSettingIsolationAsNameAlt() throws Exception {
		Properties properties = Environment.getProperties();
		augmentConfigurationSettings( properties );
		properties.put( AvailableSettings.ISOLATION, "SERIALIZABLE" );

		ConnectionProvider provider = getConnectionProviderUnderTest();

		try {
			( (Configurable) provider ).configure( properties );

			if ( Startable.class.isInstance( provider ) ) {
				( (Startable) provider ).start();
			}

			Connection connection = provider.getConnection();
			assertEquals( Connection.TRANSACTION_SERIALIZABLE, connection.getTransactionIsolation() );
			provider.closeConnection( connection );
		}
		finally {
			( (Stoppable) provider ).stop();
		}
	}
}
