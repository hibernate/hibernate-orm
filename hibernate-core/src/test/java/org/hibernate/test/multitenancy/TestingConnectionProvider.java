/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.multitenancy;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

/**
 * @author Steve Ebersole
 */
public class TestingConnectionProvider extends AbstractMultiTenantConnectionProvider {
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
