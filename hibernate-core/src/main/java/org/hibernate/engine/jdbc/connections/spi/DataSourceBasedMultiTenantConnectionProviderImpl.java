/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.connections.spi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.Context;
import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

import static org.hibernate.cfg.MultiTenancySettings.TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY;

/**
 * A concrete implementation of the {@link MultiTenantConnectionProvider} contract bases on a number of
 * reasonable assumptions.  We assume that:<ul>
 *     <li>
 *         The {@link DataSource} instances are all available from JNDI named by the tenant identifier relative
 *         to a single base JNDI context
 *     </li>
 *     <li>
 *         {@value AvailableSettings#DATASOURCE} is a string naming either the {@literal any}
 *         data source or the base JNDI context.  If the latter, {@link MultiTenancySettings#TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY} must
 *         also be set.
 *     </li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class DataSourceBasedMultiTenantConnectionProviderImpl<T>
		extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl<T>
		implements ServiceRegistryAwareService, Stoppable {

	private Map<T, DataSource> dataSourceMap;
	private JndiService jndiService;
	private T tenantIdentifierForAny;
	private String baseJndiNamespace;

	private DatabaseConnectionInfo dbInfo;

	@Override
	protected DataSource selectAnyDataSource() {
		return selectDataSource( tenantIdentifierForAny );
	}

	@Override
	protected DataSource selectDataSource(T tenantIdentifier) {
		DataSource dataSource = dataSourceMap().get( tenantIdentifier );
		if ( dataSource == null ) {
			dataSource = (DataSource) jndiService.locate( baseJndiNamespace + '/' + tenantIdentifier );
			dataSourceMap().put( tenantIdentifier, dataSource );
		}
		dbInfo = new DatabaseConnectionInfoImpl().setDBUrl( "Connecting through datasource" + baseJndiNamespace + '/' + tenantIdentifier );
		return dataSource;
	}

	private Map<T, DataSource> dataSourceMap() {
		if ( dataSourceMap == null ) {
			dataSourceMap = new ConcurrentHashMap<>();
		}
		return dataSourceMap;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		final Object dataSourceConfigValue =
				serviceRegistry.requireService( ConfigurationService.class )
						.getSettings().get( AvailableSettings.DATASOURCE );
		if ( !(dataSourceConfigValue instanceof String) ) {
			throw new HibernateException( "Improper set up of DataSourceBasedMultiTenantConnectionProviderImpl" );
		}
		final String jndiName = (String) dataSourceConfigValue;

		jndiService = serviceRegistry.getService( JndiService.class );
		if ( jndiService == null ) {
			throw new HibernateException( "Could not locate JndiService from DataSourceBasedMultiTenantConnectionProviderImpl" );
		}

		final Object namedObject = jndiService.locate( jndiName );
		if ( namedObject == null ) {
			throw new HibernateException( "JNDI name [" + jndiName + "] could not be resolved" );
		}

		if ( namedObject instanceof DataSource ) {
			final int loc = jndiName.lastIndexOf( '/' );
			this.baseJndiNamespace = jndiName.substring( 0, loc );
			this.tenantIdentifierForAny = (T) jndiName.substring( loc + 1 );
			dataSourceMap().put( tenantIdentifierForAny, (DataSource) namedObject );
		}
		else if ( namedObject instanceof Context ) {
			this.baseJndiNamespace = jndiName;
			this.tenantIdentifierForAny = (T) serviceRegistry.requireService( ConfigurationService.class )
					.getSettings()
					.get( TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY );
			if ( tenantIdentifierForAny == null ) {
				throw new HibernateException( "JNDI name named a Context, but tenant identifier to use for ANY was not specified" );
			}
		}
		else {
			throw new HibernateException(
					"Unknown object type [" + namedObject.getClass().getName() +
							"] found in JNDI location [" + jndiName + "]"
			);
		}
	}

	@Override
	public void stop() {
		if ( dataSourceMap != null ) {
			dataSourceMap.clear();
			dataSourceMap = null;
		}
	}

	@Override
	public DatabaseConnectionInfo getDatabaseConnectionInfo() {
		return dbInfo;
	}

}
