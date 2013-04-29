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
package org.hibernate.engine.jdbc.connections.spi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.Context;
import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

/**
 * A concrete implementation of the {@link MultiTenantConnectionProvider} contract bases on a number of
 * reasonable assumptions.  We assume that:<ul>
 *     <li>
 *         The {@link DataSource} instances are all available from JNDI named by the tenant identifier relative
 *         to a single base JNDI context
 *     </li>
 *     <li>
 *         {@link org.hibernate.cfg.AvailableSettings#DATASOURCE} is a string naming either the {@literal any}
 *         data source or the base JNDI context.  If the latter, {@link #TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY} must
 *         also be set.
 *     </li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class DataSourceBasedMultiTenantConnectionProviderImpl
		extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl
		implements ServiceRegistryAwareService, Stoppable {

	/**
	 * Identifies the DataSource name to use for {@link #selectAnyDataSource} handling
	 */
	public static final String TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY = "hibernate.multi_tenant.datasource.identifier_for_any";

	private Map<String,DataSource> dataSourceMap;
	private JndiService jndiService;
	private String tenantIdentifierForAny;
	private String baseJndiNamespace;

	@Override
	protected DataSource selectAnyDataSource() {
		return selectDataSource( tenantIdentifierForAny );
	}

	@Override
	protected DataSource selectDataSource(String tenantIdentifier) {
		DataSource dataSource = dataSourceMap().get( tenantIdentifier );
		if ( dataSource == null ) {
			dataSource = (DataSource) jndiService.locate( baseJndiNamespace + '/' + tenantIdentifier );
			dataSourceMap().put( tenantIdentifier, dataSource );
		}
		return dataSource;
	}

	private Map<String,DataSource> dataSourceMap() {
		if ( dataSourceMap == null ) {
			dataSourceMap = new ConcurrentHashMap<String, DataSource>();
		}
		return dataSourceMap;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		final Object dataSourceConfigValue = serviceRegistry.getService( ConfigurationService.class )
				.getSettings()
				.get( AvailableSettings.DATASOURCE );
		if ( dataSourceConfigValue == null || ! String.class.isInstance( dataSourceConfigValue ) ) {
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

		if ( DataSource.class.isInstance( namedObject ) ) {
			final int loc = jndiName.lastIndexOf( "/" );
			this.baseJndiNamespace = jndiName.substring( 0, loc );
			this.tenantIdentifierForAny = jndiName.substring( loc + 1 );
			dataSourceMap().put( tenantIdentifierForAny, (DataSource) namedObject );
		}
		else if ( Context.class.isInstance( namedObject ) ) {
			this.baseJndiNamespace = jndiName;
			this.tenantIdentifierForAny = (String) serviceRegistry.getService( ConfigurationService.class )
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
}
