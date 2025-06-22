/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy.schema;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Steve Ebersole
 */
@RequiresDialectFeature( value = ConnectionProviderBuilder.class )
public class SchemaBasedMultiTenancyTest extends AbstractSchemaBasedMultiTenancyTest<
		AbstractMultiTenantConnectionProvider<String>, ConnectionProvider> {

	protected AbstractMultiTenantConnectionProvider<String> buildMultiTenantConnectionProvider() {
		acmeProvider = ConnectionProviderBuilder.buildConnectionProvider( "acme" );
		jbossProvider = ConnectionProviderBuilder.buildConnectionProvider( "jboss" );
		return new AbstractMultiTenantConnectionProvider<>() {
			@Override
			protected ConnectionProvider getAnyConnectionProvider() {
				return acmeProvider;
			}

			@Override
			protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
				if ( "acme".equals( tenantIdentifier ) ) {
					return acmeProvider;
				}
				else if ( "jboss".equals( tenantIdentifier ) ) {
					return jbossProvider;
				}
				throw new HibernateException( "Unknown tenant identifier" );
			}
		};
	}

	@Test
	@JiraKey( value = "HHH-11651")
	public void testUnwrappingConnectionProvider() {
		final MultiTenantConnectionProvider<String> multiTenantConnectionProvider = serviceRegistry.getService(
				MultiTenantConnectionProvider.class );
		final ConnectionProvider connectionProvider = multiTenantConnectionProvider.unwrap( ConnectionProvider.class );
		assertThat( connectionProvider, is( notNullValue() ) );
	}

	@Test
	@JiraKey(value = "HHH-11651")
	public void testUnwrappingAbstractMultiTenantConnectionProvider() {
		final MultiTenantConnectionProvider<String> multiTenantConnectionProvider = serviceRegistry.getService(
				MultiTenantConnectionProvider.class );
		final AbstractMultiTenantConnectionProvider<?> connectionProvider = multiTenantConnectionProvider.unwrap(
				AbstractMultiTenantConnectionProvider.class );
		assertThat( connectionProvider, is( notNullValue() ) );
	}

	@Test
	@JiraKey(value = "HHH-11651")
	public void testUnwrappingMultiTenantConnectionProvider() {
		final MultiTenantConnectionProvider<String> multiTenantConnectionProvider = serviceRegistry.getService(
				MultiTenantConnectionProvider.class );
		final MultiTenantConnectionProvider<String> connectionProvider = multiTenantConnectionProvider.unwrap(
				MultiTenantConnectionProvider.class );
		assertThat( connectionProvider, is( notNullValue() ) );
	}
}
