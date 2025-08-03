/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.spi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.Context;
import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

import static org.hibernate.cfg.JdbcSettings.DATASOURCE;
import static org.hibernate.cfg.MultiTenancySettings.TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY;

/**
 * A concrete implementation of the {@link MultiTenantConnectionProvider} contract bases on
 * a number of reasonable assumptions. We assume that:<ul>
 *     <li>
 *         The {@link DataSource} instances are all available from JNDI named by the tenant
 *         identifier relative to a single base JNDI context.
 *     </li>
 *     <li>
 *         {@value AvailableSettings#DATASOURCE} is a string naming either the {@literal any}
 *         data source or the base JNDI context. If the latter,
 *         {@link MultiTenancySettings#TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY} must also be set.
 *     </li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class DataSourceBasedMultiTenantConnectionProviderImpl<T>
		extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl<T>
		implements ServiceRegistryAwareService, Stoppable {

	private final Map<T, DataSource> dataSourceMap = new ConcurrentHashMap<>();
	private JndiService jndiService;
	private T tenantIdentifierForAny;
	private String baseJndiNamespace;
	private String jndiName;

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
		return dataSource;
	}

	private Map<T, DataSource> dataSourceMap() {
		return dataSourceMap;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		final ConfigurationService configurationService = serviceRegistry.requireService( ConfigurationService.class );
		final Object dataSourceConfigValue = configurationService.getSettings().get( DATASOURCE );
		if ( !(dataSourceConfigValue instanceof String configuredJndiName) ) {
			throw new HibernateException( "illegal value for configuration setting '" + DATASOURCE + "'" );
		}
		jndiName = configuredJndiName;

		jndiService = serviceRegistry.getService( JndiService.class );
		if ( jndiService == null ) {
			throw new HibernateException( "Could not locate JndiService from DataSourceBasedMultiTenantConnectionProviderImpl" );
		}

		final Object namedObject = jndiService.locate( this.jndiName );
		if ( namedObject == null ) {
			throw new HibernateException( "JNDI name [" + this.jndiName + "] could not be resolved" );
		}
		else if ( namedObject instanceof DataSource datasource ) {
			final int loc = jndiName.lastIndexOf( '/' );
			baseJndiNamespace = jndiName.substring( 0, loc );
			final String prefix = jndiName.substring( loc + 1);
			tenantIdentifierForAny = (T) prefix;
			dataSourceMap().put( tenantIdentifierForAny, datasource );
		}
		else if ( namedObject instanceof Context ) {
			baseJndiNamespace = jndiName;
			final Object configuredTenantId =
					configurationService.getSettings().get( TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY );
			tenantIdentifierForAny = (T) configuredTenantId;
			if ( tenantIdentifierForAny == null ) {
				throw new HibernateException( "JNDI name named a Context, but tenant identifier to use for ANY was not specified" );
			}
		}
		else {
			throw new HibernateException(
					"Unknown object type [" + namedObject.getClass().getName() +
					"] found in JNDI location [" + this.jndiName + "]"
			);
		}
	}

	@Override
	public void stop() {
		dataSourceMap.clear();
	}

	@Override
	public DatabaseConnectionInfo getDatabaseConnectionInfo(Dialect dialect) {
		return new DatabaseConnectionInfoImpl(
				null,
				null,
				null,
				dialect.getClass(),
				dialect.getVersion(),
				null,
				null,
				null,
				null,
				null,
				null,
				null
		) {
			@Override
			public String toInfoString() {
				return "\tMulti-tenant datasource JNDI name [" + jndiName + ']';
			}
		};
	}
}
