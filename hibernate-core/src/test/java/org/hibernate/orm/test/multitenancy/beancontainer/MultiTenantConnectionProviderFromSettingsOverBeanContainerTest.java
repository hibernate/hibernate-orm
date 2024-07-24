/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy.beancontainer;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.orm.test.multitenancy.ConfigurableMultiTenantConnectionProvider;

import org.hibernate.testing.orm.junit.RequiresDialect;

/**
 * @author Yanming Zhou
 */
@RequiresDialect(H2Dialect.class)
public class MultiTenantConnectionProviderFromSettingsOverBeanContainerTest extends MultiTenantConnectionProviderFromBeanContainerTest {

	private ConfigurableMultiTenantConnectionProvider providerFromSettings;

	@Override
	protected Map<String, Object> createSettings() {
		Map<String, Object> settings = super.createSettings();
		providerFromSettings = new ConfigurableMultiTenantConnectionProvider(connectionProviderMap);
		settings.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, providerFromSettings);
		return settings;
	}

	@Override
	protected MultiTenantConnectionProvider<?> expectedProviderInUse() {
		return providerFromSettings;
	}
}
