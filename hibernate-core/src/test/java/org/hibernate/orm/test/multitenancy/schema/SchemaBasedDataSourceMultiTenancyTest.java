/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy.schema;

import org.hibernate.HibernateException;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public class SchemaBasedDataSourceMultiTenancyTest extends AbstractSchemaBasedMultiTenancyTest<
		AbstractDataSourceBasedMultiTenantConnectionProviderImpl<String>, ConnectionProvider> {

	protected AbstractDataSourceBasedMultiTenantConnectionProviderImpl<String> buildMultiTenantConnectionProvider() {
		acmeProvider = ConnectionProviderBuilder.buildDataSourceConnectionProvider( "acme" );
		jbossProvider = ConnectionProviderBuilder.buildDataSourceConnectionProvider( "jboss" );
		return new AbstractDataSourceBasedMultiTenantConnectionProviderImpl<>() {
			@Override
			protected DataSource selectAnyDataSource() {
				return acmeProvider.unwrap( DataSource.class );
			}

			@Override
			protected DataSource selectDataSource(String tenantIdentifier) {
				if ( "acme".equals( tenantIdentifier ) ) {
					return acmeProvider.unwrap( DataSource.class );
				}
				else if ( "jboss".equals( tenantIdentifier ) ) {
					return jbossProvider.unwrap( DataSource.class );
				}
				throw new HibernateException( "Unknown tenant identifier" );
			}
		};
	}

	@Test
	@JiraKey(value = "HHH-11651")
	public void testUnwrappingConnectionProvider() {
		final MultiTenantConnectionProvider<String> multiTenantConnectionProvider = serviceRegistry.getService(
				MultiTenantConnectionProvider.class );
		final DataSource dataSource = multiTenantConnectionProvider.unwrap( DataSource.class );
		assertThat( dataSource ).isNotNull();
	}

	@Test
	@JiraKey(value = "HHH-11651")
	public void testUnwrappingAbstractMultiTenantConnectionProvider() {
		final MultiTenantConnectionProvider<String> multiTenantConnectionProvider = serviceRegistry.getService(
				MultiTenantConnectionProvider.class );
		final AbstractDataSourceBasedMultiTenantConnectionProviderImpl dataSourceBasedMultiTenantConnectionProvider = multiTenantConnectionProvider.unwrap(
				AbstractDataSourceBasedMultiTenantConnectionProviderImpl.class );
		assertThat( dataSourceBasedMultiTenantConnectionProvider ).isNotNull();
	}

	@Test
	@JiraKey(value = "HHH-11651")
	public void testUnwrappingMultiTenantConnectionProvider() {
		final MultiTenantConnectionProvider<String> multiTenantConnectionProvider = serviceRegistry.getService(
				MultiTenantConnectionProvider.class );
		final MultiTenantConnectionProvider<String> connectionProvider = multiTenantConnectionProvider.unwrap(
				MultiTenantConnectionProvider.class );
		assertThat( connectionProvider ).isNotNull();
	}
}
