/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.multitenancy.schema;

import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialectFeature( value = ConnectionProviderBuilder.class )
public class SchemaBasedDataSourceMultiTenancyTest  extends AbstractSchemaBasedMultiTenancyTest<
		AbstractDataSourceBasedMultiTenantConnectionProviderImpl, DatasourceConnectionProviderImpl> {

	protected AbstractDataSourceBasedMultiTenantConnectionProviderImpl buildMultiTenantConnectionProvider() {
		acmeProvider = ConnectionProviderBuilder.buildDataSourceConnectionProvider( "acme" );
		jbossProvider = ConnectionProviderBuilder.buildDataSourceConnectionProvider( "jboss" );
		return new AbstractDataSourceBasedMultiTenantConnectionProviderImpl() {
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
	@TestForIssue( jiraKey = "HHH-11651")
	public void testUnwrappingConnectionProvider() {
		final MultiTenantConnectionProvider multiTenantConnectionProvider = serviceRegistry.getService(
				MultiTenantConnectionProvider.class );
		final DataSource dataSource = multiTenantConnectionProvider.unwrap( DataSource.class );
		assertThat( dataSource, is( notNullValue() ) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11651")
	public void testUnwrappingAbstractMultiTenantConnectionProvider() {
		final MultiTenantConnectionProvider multiTenantConnectionProvider = serviceRegistry.getService(
				MultiTenantConnectionProvider.class );
		final AbstractDataSourceBasedMultiTenantConnectionProviderImpl dataSourceBasedMultiTenantConnectionProvider = multiTenantConnectionProvider.unwrap(
				AbstractDataSourceBasedMultiTenantConnectionProviderImpl.class );
		assertThat( dataSourceBasedMultiTenantConnectionProvider, is( notNullValue() ) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11651")
	public void testUnwrappingMultiTenantConnectionProvider() {
		final MultiTenantConnectionProvider multiTenantConnectionProvider = serviceRegistry.getService(
				MultiTenantConnectionProvider.class );
		final MultiTenantConnectionProvider connectionProvider = multiTenantConnectionProvider.unwrap(
				MultiTenantConnectionProvider.class );
		assertThat( connectionProvider, is( notNullValue() ) );
	}
}
