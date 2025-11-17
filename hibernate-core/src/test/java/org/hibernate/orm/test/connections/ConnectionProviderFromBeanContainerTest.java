/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.connections;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER;
import static org.junit.Assert.assertSame;

/**
 * @author Yanming Zhou
 */
@RequiresDialect(H2Dialect.class)
public class ConnectionProviderFromBeanContainerTest extends BaseUnitTestCase {

	private final ConnectionProvider dummyConnectionProvider = new DummyConnectionProvider();

	private Map<String, Object> createSettings() {
		Map<String, Object> settings = new HashMap<>();
		settings.put( AvailableSettings.ALLOW_EXTENSIONS_IN_CDI, "true" );
		settings.put( AvailableSettings.BEAN_CONTAINER, new BeanContainer() {
			@Override
			@SuppressWarnings("unchecked")
			public <B> ContainedBean<B> getBean(
					Class<B> beanType,
					LifecycleOptions lifecycleOptions,
					BeanInstanceProducer fallbackProducer) {
				return new ContainedBean<>() {
					@Override
					public B getBeanInstance() {
						return (B) (beanType == DummyConnectionProvider.class ?
								dummyConnectionProvider : fallbackProducer.produceBeanInstance( beanType ) );
					}

					@Override
					public Class<B> getBeanClass() {
						return beanType;
					}
				};
			}
			@Override
			public <B> ContainedBean<B> getBean(
					String name,
					Class<B> beanType,
					LifecycleOptions lifecycleOptions,
					BeanInstanceProducer fallbackProducer) {
				return new ContainedBean<>() {
					@Override
					public B getBeanInstance() {
						return fallbackProducer.produceBeanInstance( beanType );
					}
					@Override
					public Class<B> getBeanClass() {
						return beanType;
					}
				};
			}

			@Override
			public void stop() {

			}
		} );
		return settings;
	}

	@Test
	public void testProviderFromBeanContainerInUse() {
		Map<String, Object> settings = createSettings();
		settings.putIfAbsent( CONNECTION_PROVIDER, DummyConnectionProvider.class.getName() );
		try ( ServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySettings( settings ).build() ) {
			ConnectionProvider providerInUse = serviceRegistry.getService( ConnectionProvider.class );
			assertSame( dummyConnectionProvider, providerInUse );
		}
	}

	public static class DummyConnectionProvider implements ConnectionProvider {

		@Override
		public boolean isUnwrappableAs(Class<?> unwrapType) {
			return false;
		}

		@Override
		public <T> T unwrap(Class<T> unwrapType) {
			return null;
		}

		@Override
		public Connection getConnection() throws SQLException {
			return null;
		}

		@Override
		public void closeConnection(Connection connection) throws SQLException {

		}

		@Override
		public boolean supportsAggressiveRelease() {
			return false;
		}
	};
}
