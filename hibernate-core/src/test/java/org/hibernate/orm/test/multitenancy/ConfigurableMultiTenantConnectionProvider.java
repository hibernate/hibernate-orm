package org.hibernate.orm.test.multitenancy;

import java.util.Map;

import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

/**
 * @author Vlad Mihalcea
 */
//tag::multitenacy-hibernate-ConfigurableMultiTenantConnectionProvider-example[]
public class ConfigurableMultiTenantConnectionProvider
		extends AbstractMultiTenantConnectionProvider<String> {

	private final Map<String, ConnectionProvider> connectionProviderMap;
	private final String bootstrapTenant;

	public ConfigurableMultiTenantConnectionProvider(Map<String, ConnectionProvider> providers) {
		this( providers, providers.keySet().stream().sorted().findFirst().orElseThrow() );
	}

	public ConfigurableMultiTenantConnectionProvider(Map<String, ConnectionProvider> providers, String bootstrapTenant) {
		this.connectionProviderMap = Map.copyOf( providers );
		this.bootstrapTenant = bootstrapTenant;
		selectConnectionProvider( bootstrapTenant );
	}

	@Override
	protected ConnectionProvider getAnyConnectionProvider() {
		return selectConnectionProvider( bootstrapTenant );
	}

	@Override
	protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
		var provider = connectionProviderMap.get( tenantIdentifier );
		if ( provider == null ) {
			throw new IllegalArgumentException( "Unknown tenant: " + tenantIdentifier );
		}
		return provider;
	}
}
//end::multitenacy-hibernate-ConfigurableMultiTenantConnectionProvider-example[]
