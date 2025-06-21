/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.spi;

import org.hibernate.HibernateException;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;

import static org.hibernate.cfg.JdbcSettings.DATASOURCE;
import static org.hibernate.cfg.MultiTenancySettings.TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY;

/**
 * A {@link MultiTenantConnectionProvider} backed by a single {@link DataSource},
 * where each tenant has a dedicated schema, and {@link Connection#setSchema(String)}
 * is used to select the appropriate schema. The name of the database schema must
 * match the {@linkplain org.hibernate.context.spi.CurrentTenantIdentifierResolver
 * tenant id}, which must be a string.
 *
 * @author Gavin King
 * @since 7.1
 */
public class SchemaBasedMultiTenantConnectionProviderImpl
		extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl<String>
		implements ServiceRegistryAwareService, Stoppable {

	private DataSource dataSource;
	private String tenantIdentifierForAny;

	@Override
	protected DataSource selectAnyDataSource() {
		return dataSource;
	}

	@Override
	protected DataSource selectDataSource(String tenantIdentifier) {
		return selectAnyDataSource();
	}

	@Override
	public Connection getAnyConnection() throws SQLException {
		final Connection connection = super.getAnyConnection();
		if ( tenantIdentifierForAny != null ) {
			connection.setSchema( tenantIdentifierForAny );
		}
		return connection;
	}

	@Override
	public Connection getConnection(String tenantIdentifier) throws SQLException {
		final Connection connection = super.getConnection( tenantIdentifier );
		connection.setSchema( tenantIdentifier );
		return connection;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		final var settings = serviceRegistry.requireService( ConfigurationService.class ).getSettings();
		final Object dataSourceConfigValue = settings.get( DATASOURCE );
		if ( !(dataSourceConfigValue instanceof String configuredJndiName) ) {
			throw new HibernateException( "illegal value for configuration setting '" + DATASOURCE + "'" );
		}

		final JndiService jndiService = serviceRegistry.getService( JndiService.class );
		if ( jndiService == null ) {
			throw new HibernateException( "Could not locate JndiService from DataSourceBasedMultiTenantConnectionProviderImpl" );
		}

		final Object namedObject = jndiService.locate( configuredJndiName );
		if ( namedObject == null ) {
			throw new HibernateException( "JNDI name [" + configuredJndiName + "] could not be resolved" );
		}
		else if ( namedObject instanceof DataSource datasource ) {
			dataSource = datasource;
		}
		else {
			throw new HibernateException(
					"Unknown object type [" + namedObject.getClass().getName() +
					"] found in JNDI location [" + configuredJndiName + "]"
			);
		}

		final Object configuredTenantId =
				settings.get( TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY );
		tenantIdentifierForAny = (String) configuredTenantId;
	}

	@Override
	public void stop() {
		dataSource = null;
	}
}
