/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.jta;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.SettingConfiguration;
import org.hibernate.testing.util.ServiceRegistryUtil;

import static org.hibernate.cfg.TransactionSettings.TRANSACTION_COORDINATOR_STRATEGY;

/**
 * @author Steve Ebersole
 */
public final class TestingJtaBootstrap implements SettingConfiguration.Configurer {
	public static final TestingJtaBootstrap INSTANCE = new TestingJtaBootstrap();

	public static void prepare(Map<String,Object> configValues) {
		configValues.put( TRANSACTION_COORDINATOR_STRATEGY, "jta" );
		configValues.put( AvailableSettings.JTA_PLATFORM, TestingJtaPlatformImpl.INSTANCE );
		configValues.put( AvailableSettings.CONNECTION_PROVIDER, JtaAwareConnectionProviderImpl.class.getName() );
		configValues.put(
				AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT,
				Boolean.TRUE
		);
		configValues.put( "javax.persistence.transactionType", "JTA" );
	}

	public static void prepare(StandardServiceRegistryBuilder registryBuilder) {
		registryBuilder.applySetting( TRANSACTION_COORDINATOR_STRATEGY, "jta" );
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

	public TestingJtaBootstrap() {
	}

	@Override
	public void applySettings(StandardServiceRegistryBuilder registryBuilder) {
		prepare( registryBuilder );
	}

	@Override
	public void applySettings(Map<String, Object> configValues) {
		prepare( configValues );
	}
}
