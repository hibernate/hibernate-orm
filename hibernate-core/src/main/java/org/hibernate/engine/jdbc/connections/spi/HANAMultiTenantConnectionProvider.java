/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.connections.spi;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

public class HANAMultiTenantConnectionProvider extends AbstractMultiTenantConnectionProvider
		implements ServiceRegistryAwareService, Stoppable, Configurable {

	private static final long serialVersionUID = 7809039247388187957L;

	private ServiceRegistryImplementor serviceRegistry;

	private ConnectionProvider systemDBConnectionProvider;
	private Map<String, ConnectionProvider> tenantConnectionProviders = new HashMap<>();

	@Override
	protected ConnectionProvider getAnyConnectionProvider() {
		if ( this.systemDBConnectionProvider == null ) {
			@SuppressWarnings("unchecked")
			Map<String, Object> configurationValues = new HashMap<>(
					this.serviceRegistry.getService( ConfigurationService.class ).getSettings() );
			configurationValues.put( AvailableSettings.MULTI_TENANT, MultiTenancyStrategy.NONE );
			this.systemDBConnectionProvider = ConnectionProviderInitiator.INSTANCE.initiateService( configurationValues,
					this.serviceRegistry );

			if ( this.systemDBConnectionProvider instanceof Configurable ) {
				Configurable configurableConnectionProvider = (Configurable) this.systemDBConnectionProvider;
				configurableConnectionProvider.configure( configurationValues );
			}
		}
		return this.systemDBConnectionProvider;
	}

	@Override
	protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
		ConnectionProvider provider = this.tenantConnectionProviders.get( tenantIdentifier );
		if ( provider == null ) {
			@SuppressWarnings("unchecked")
			Map<String, Object> configurationValues = new HashMap<>(
					this.serviceRegistry.getService( ConfigurationService.class ).getSettings() );

			String urlString = (String) configurationValues.get( AvailableSettings.URL );
			String query = getConnectionURLQueryPart( urlString );
			if ( query == null ) {
				urlString += "?databaseName=" + tenantIdentifier;
			}
			else {
				urlString += "&databaseName=" + tenantIdentifier;
			}

			configurationValues.put( AvailableSettings.URL, urlString );
			configurationValues.put( AvailableSettings.MULTI_TENANT, MultiTenancyStrategy.NONE );

			provider = ConnectionProviderInitiator.INSTANCE.initiateService( configurationValues, this.serviceRegistry );

			if ( provider instanceof Configurable ) {
				Configurable configurableConnectionProvider = (Configurable) provider;
				configurableConnectionProvider.configure( configurationValues );
			}
			this.tenantConnectionProviders.put( tenantIdentifier, provider );
		}
		return provider;
	}

	@Override
	public void stop() {
		if ( this.systemDBConnectionProvider instanceof Stoppable ) {
			( (Stoppable) this.systemDBConnectionProvider ).stop();
		}

		this.tenantConnectionProviders.forEach( (tenant, provider) -> {
			if ( provider instanceof Stoppable ) {
				( (Stoppable) provider ).stop();
			}
		} );
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void configure(Map configurationValues) {
		final MultiTenancyStrategy strategy = MultiTenancyStrategy.determineMultiTenancyStrategy( configurationValues );
		if ( strategy != MultiTenancyStrategy.DATABASE ) {
			throw new HibernateException(
					"The multi-tenancy strategy [" + strategy.name() + "] is not supported by HANA" );
		}

		String urlString = (String) configurationValues.get( AvailableSettings.URL );
		String query = getConnectionURLQueryPart( urlString );
		if ( query != null ) {
			boolean databaseNameParameterExists = Arrays.stream( query.split( "&" ) )
					.anyMatch( parameter -> "databasename".equals( parameter.toLowerCase() )
							|| parameter.toLowerCase().startsWith( "databasename=" ) );
			if ( databaseNameParameterExists ) {
				throw new HibernateException(
						"The connection URL [" + urlString + "] must not contain the 'databaseName' parameter" );
			}
		}
	}

	private String getConnectionURLQueryPart(String urlString) {
		try {
			URI uri = new URI( urlString );
			if ( !"jdbc".equals( uri.getScheme() ) ) {
				throw new HibernateException( "The connection URL [" + urlString + "] must start with 'jdbc:'" );
			}
			URI innerURI = new URI( uri.getSchemeSpecificPart() );
			return innerURI.getQuery();
		}
		catch (URISyntaxException e) {
			throw new HibernateException( "The connection URL [" + urlString + "] is invalid", e );
		}
	}

}
