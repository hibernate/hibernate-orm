/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.proxool;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.TiDBDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.proxool.internal.ProxoolConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.common.connections.BaseTransactionIsolationConfigTest;
import org.junit.Before;

/**
 * @author Steve Ebersole
 */
@SkipForDialect(value = TiDBDialect.class, comment = "Doesn't support SERIALIZABLE isolation")
public class ProxoolTransactionIsolationConfigTest extends BaseTransactionIsolationConfigTest {
	private Map<String,Object> properties;
	private StandardServiceRegistry ssr;

	@Before
	public void setUp() {
		String poolName = "pool-one";

		properties = new HashMap<>();
		properties.put( AvailableSettings.PROXOOL_POOL_ALIAS, poolName );
		properties.put( AvailableSettings.PROXOOL_PROPERTIES, poolName + ".properties" );

		ssr = new StandardServiceRegistryBuilder()
				.applySettings( properties )
				.build();
	}

	@Override
	protected ConnectionProvider getConnectionProviderUnderTest() {
		ProxoolConnectionProvider provider = new ProxoolConnectionProvider();
		provider.injectServices( (ServiceRegistryImplementor) ssr );
		return provider;
	}

	@Override
	protected void augmentConfigurationSettings(Properties properties) {
		super.augmentConfigurationSettings( properties );

		properties.putAll( this.properties );
	}
}
