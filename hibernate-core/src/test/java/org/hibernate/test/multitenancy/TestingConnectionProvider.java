/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
