package org.hibernate.ejb.test.multitenancy;

import java.util.HashMap;

import org.hibernate.HibernateException;
import org.hibernate.service.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.service.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;

public class TestMultiTenantConnectionProvider extends AbstractMultiTenantConnectionProvider{
	
	private HashMap<String, ConnectionProvider> providerMap=new HashMap<String, ConnectionProvider>();
	
	@Override
	protected ConnectionProvider getAnyConnectionProvider() {
		return providerMap.entrySet().iterator().next().getValue();
	}

	@Override
	protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
		ConnectionProvider provider=providerMap.get(tenantIdentifier);
		if (provider==null)
			throw new HibernateException( "Unknown tenant identifier" );
		return provider;
	}

	public void addProvider(String tenantIdentifier,
			DriverManagerConnectionProviderImpl connectionProvider) {
		providerMap.put(tenantIdentifier, connectionProvider);
		
	}
}