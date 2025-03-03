/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.common.connections;

import java.sql.Connection;
import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.util.PropertiesHelper;
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
	public void testSettingIsolationAsNumericString() throws Exception {
		Properties properties = Environment.getProperties();
		augmentConfigurationSettings( properties );
		properties.put( AvailableSettings.ISOLATION, Integer.toString( Connection.TRANSACTION_SERIALIZABLE ) );

		ConnectionProvider provider = getConnectionProviderUnderTest();

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
	public void testSettingIsolationAsName() throws Exception {
		Properties properties = Environment.getProperties();
		augmentConfigurationSettings( properties );
		properties.put( AvailableSettings.ISOLATION, "TRANSACTION_SERIALIZABLE" );

		ConnectionProvider provider = getConnectionProviderUnderTest();

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
	public void testSettingIsolationAsNameAlt() throws Exception {
		Properties properties = Environment.getProperties();
		augmentConfigurationSettings( properties );
		properties.put( AvailableSettings.ISOLATION, "SERIALIZABLE" );

		ConnectionProvider provider = getConnectionProviderUnderTest();

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
