/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.jta;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.util.ServiceRegistryUtil;

/**
 * @author Steve Ebersole
 */
public final class TestingJtaBootstrap {
	public static final TestingJtaBootstrap INSTANCE = new TestingJtaBootstrap();

	public static void prepare(Map<String,Object> configValues) {
		configValues.put( AvailableSettings.JTA_PLATFORM, TestingJtaPlatformImpl.INSTANCE );
		configValues.put( AvailableSettings.CONNECTION_PROVIDER, JtaAwareConnectionProviderImpl.class.getName() );
		configValues.put(
				AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT,
				Boolean.TRUE
		);
		configValues.put( "javax.persistence.transactionType", "JTA" );
	}

	public static void prepare(StandardServiceRegistryBuilder registryBuilder) {
		registryBuilder.applySetting( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta" );
		registryBuilder.applySetting( AvailableSettings.JTA_PLATFORM, TestingJtaPlatformImpl.INSTANCE );
		registryBuilder.applySetting( AvailableSettings.CONNECTION_PROVIDER, JtaAwareConnectionProviderImpl.class.getName() );
		registryBuilder.applySetting(
				AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT,
				Boolean.TRUE
		);
		registryBuilder.applySetting( "javax.persistence.transactionType", "JTA" );
	}

	public static StandardServiceRegistryBuilder prepare() {
		final StandardServiceRegistryBuilder registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		prepare( registryBuilder );
		return registryBuilder;
	}

	private TestingJtaBootstrap() {
	}
}
