/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.proxool;

import java.util.Properties;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.proxool.internal.ProxoolConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.hibernate.testing.common.connections.BaseTransactionIsolationConfigTest;
import org.junit.Before;

/**
 * @author Steve Ebersole
 */
public class ProxoolTransactionIsolationConfigTest extends BaseTransactionIsolationConfigTest {
	private Properties properties;
	private StandardServiceRegistry ssr;

	@Before
	public void setUp() {
		String poolName = "pool-one";

		properties = new Properties();
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
