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
 * @author Steve Ebersole
 */
public class TestingConnectionProvider extends AbstractMultiTenantConnectionProvider<String> {
	private Map<String,ConnectionProvider> connectionProviderMap;

	public TestingConnectionProvider(Map<String, ConnectionProvider> connectionProviderMap) {
		this.connectionProviderMap = connectionProviderMap;
	}

	public TestingConnectionProvider(NamedConnectionProviderPair... pairs) {
		Map<String,ConnectionProvider> map = new HashMap<String, ConnectionProvider>();
		for ( NamedConnectionProviderPair pair : pairs ) {
			map.put( pair.name, pair.connectionProvider );
		}
		this.connectionProviderMap = map;
	}

	@Override
	protected ConnectionProvider getAnyConnectionProvider() {
		return connectionProviderMap.values().iterator().next();
	}

	@Override
	protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
		return connectionProviderMap.get( tenantIdentifier );
	}

	public static class NamedConnectionProviderPair {
		private final String name;
		private final ConnectionProvider connectionProvider;

		public NamedConnectionProviderPair(String name, ConnectionProvider connectionProvider) {
			this.name = name;
			this.connectionProvider = connectionProvider;
		}
	}
}
