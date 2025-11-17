/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.common;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
@ServiceRegistry
public abstract class BaseTransactionIsolationConfigTest {
	protected abstract ConnectionProvider getConnectionProviderUnderTest(ServiceRegistryScope registryScope);

	@Test
	public void testSettingIsolationAsNumeric(ServiceRegistryScope registryScope) throws Exception {
		Properties properties = Environment.getProperties();
		properties.put( AvailableSettings.ISOLATION, Connection.TRANSACTION_SERIALIZABLE );

		ConnectionProvider provider = getConnectionProviderUnderTest( registryScope );

		try {
			( (Configurable) provider ).configure( PropertiesHelper.map( properties ) );

			if ( provider instanceof Startable ) {
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
	public void testSettingIsolationAsNumericString(ServiceRegistryScope registryScope) throws Exception {
		Properties properties = Environment.getProperties();
		properties.put( AvailableSettings.ISOLATION, Integer.toString( Connection.TRANSACTION_SERIALIZABLE ) );

		ConnectionProvider provider = getConnectionProviderUnderTest( registryScope );

		try {
			( (Configurable) provider ).configure( PropertiesHelper.map( properties ) );

			if ( provider instanceof Startable ) {
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
	public void testSettingIsolationAsName(ServiceRegistryScope registryScope) throws Exception {
		Properties properties = Environment.getProperties();
		properties.put( AvailableSettings.ISOLATION, "TRANSACTION_SERIALIZABLE" );

		ConnectionProvider provider = getConnectionProviderUnderTest( registryScope );

		try {
			( (Configurable) provider ).configure( PropertiesHelper.map( properties ) );

			if ( provider instanceof Startable ) {
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
	public void testSettingIsolationAsNameAlt(ServiceRegistryScope registryScope) throws Exception {
		Properties properties = Environment.getProperties();
		properties.put( AvailableSettings.ISOLATION, "SERIALIZABLE" );

		ConnectionProvider provider = getConnectionProviderUnderTest( registryScope );

		try {
			( (Configurable) provider ).configure( PropertiesHelper.map( properties ) );

			if ( provider instanceof Startable ) {
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
