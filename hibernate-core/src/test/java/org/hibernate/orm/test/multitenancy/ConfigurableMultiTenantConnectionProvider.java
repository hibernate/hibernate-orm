/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

/**
 * @author Vlad Mihalcea
 */
//tag::multitenacy-hibernate-ConfigurableMultiTenantConnectionProvider-example[]
public class ConfigurableMultiTenantConnectionProvider
		extends AbstractMultiTenantConnectionProvider<String> {

	private final Map<String, ConnectionProvider> connectionProviderMap =
		new HashMap<>();

	public ConfigurableMultiTenantConnectionProvider(
			Map<String, ConnectionProvider> connectionProviderMap) {
		this.connectionProviderMap.putAll(connectionProviderMap);
	}

	@Override
	protected ConnectionProvider getAnyConnectionProvider() {
		return connectionProviderMap.values().iterator().next();
	}

	@Override
	protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
		return connectionProviderMap.get(tenantIdentifier);
	}
}
//end::multitenacy-hibernate-ConfigurableMultiTenantConnectionProvider-example[]
