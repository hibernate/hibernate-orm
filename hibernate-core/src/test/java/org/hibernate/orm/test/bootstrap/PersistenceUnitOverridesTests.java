/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bootstrap;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.HibernatePersistenceProvider;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.jpa.DelegatingPersistenceUnitInfo;
import org.hibernate.testing.util.jpa.PersistenceUnitInfoAdapter;
import org.hibernate.testing.util.jpa.PersistenceUnitInfoPropertiesWrapper;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class PersistenceUnitOverridesTests extends BaseUnitTestCase {

	@Test
	public void testCustomProviderPassingIntegrationJpaJdbcOverrides() {
		PersistenceProvider provider = new HibernatePersistenceProvider() {
			@Override
			public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map integrationOverrides) {
				return super.createContainerEntityManagerFactory(
						new DelegatingPersistenceUnitInfo( info ) {
							@Override
							public Properties getProperties() {
								// use the "db1" connection settings keyed by the JPA property names (org.hibernate.cfg.AvailableSettings.JPA_JDBC_DRIVER, e.g.)
								final Properties properties = new Properties();
								properties.putAll( info.getProperties() );
								properties.putAll( ConnectionProviderBuilder.getJpaConnectionProviderProperties( "db1" ) );
								return properties;
							}
						},
						integrationOverrides
				);
			}
		};

		// however, use the "db2" JPA connection settings which should override the persistence unit values
		final Map integrationOverrides = ConnectionProviderBuilder.getJpaConnectionProviderProperties( "db2" );

		final EntityManagerFactory emf = provider.createContainerEntityManagerFactory(
				new PersistenceUnitInfoPropertiesWrapper(),
				integrationOverrides
		);

		try {
			final Map<String, Object> properties = emf.getProperties();

			final Object hibernateJdbcDriver = properties.get( AvailableSettings.URL );
			assertThat( hibernateJdbcDriver, notNullValue() );

			final Object jpaJdbcDriver = properties.get( AvailableSettings.JPA_JDBC_URL );
			assertThat( (String) jpaJdbcDriver, containsString( "db2" ) );
		}
		finally {
			emf.close();
		}
	}

	@Test
	public void testPassingIntegrationJpaJdbcOverridesForJtaDataSourceProperty() {
		PersistenceProvider provider = new HibernatePersistenceProvider() {
			@Override
			public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map integrationOverrides) {
				return super.createContainerEntityManagerFactory(
						new DelegatingPersistenceUnitInfo( info ) {

							// inject a JPA JTA DataSource setting into the PU
							final DataSource puDataSource;
							final Properties puProperties;

							{
								puDataSource = new DataSourceStub( "puDataSource" );

								puProperties = new Properties();
								puProperties.putAll( info.getProperties() );
								puProperties.put( AvailableSettings.JPA_JTA_DATASOURCE, puDataSource );
							}

							@Override
							public Properties getProperties() {
								return puProperties;
							}
						},
						integrationOverrides
				);
			}
		};

		final EntityManagerFactory emf = provider.createContainerEntityManagerFactory(
				new PersistenceUnitInfoAdapter(),
				// however, provide JPA connection settings as "integration settings", which according to JPA spec should override the persistence unit values.
				//		- note that it is unclear in the spec whether JDBC value in the integration settings should override
				//			a JTA DataSource (nor the reverse).  However, that is a useful thing to support
				ConnectionProviderBuilder.getJpaConnectionProviderProperties( "db2" )
		);

		try {
			final Map<String, Object> properties = emf.getProperties();

			final Object hibernateJdbcDriver = properties.get( AvailableSettings.URL );
			assertThat( hibernateJdbcDriver, notNullValue() );

			final Object jpaJdbcDriver = properties.get( AvailableSettings.JPA_JDBC_URL );
			assertThat( (String) jpaJdbcDriver, containsString( "db2" ) );

			// see if the values had the affect to adjust the `ConnectionProvider` used
			final ConnectionProvider connectionProvider = emf.unwrap( SessionFactoryImplementor.class )
					.getServiceRegistry()
					.getService( ConnectionProvider.class );
			assertThat( connectionProvider, instanceOf( DriverManagerConnectionProviderImpl.class ) );
		}
		finally {
			emf.close();
		}
	}

	@Test
	@FailureExpected(
			jiraKey = "HHH-12858",
			message = "Even though the JDBC settings override a DataSource *property*, it" +
					" does not override a DataSource defined using the dedicated persistence.xml element"
	)
	public void testPassingIntegrationJpaJdbcOverridesForJtaDataSourceElement() {
		PersistenceProvider provider = new HibernatePersistenceProvider() {
			@Override
			public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map integrationOverrides) {
				return super.createContainerEntityManagerFactory(
						new DelegatingPersistenceUnitInfo( info ) {
							// inject a JPA JTA DataSource setting into the PU
							final DataSource puDataSource;

							{
								puDataSource = new DataSourceStub( "puDataSource" );
							}

							@Override
							public DataSource getJtaDataSource() {
								return puDataSource;
							}
						},
						integrationOverrides
				);
			}
		};

		final EntityManagerFactory emf = provider.createContainerEntityManagerFactory(
				new PersistenceUnitInfoAdapter(),
				// however, provide JPA connection settings as "integration settings", which according to JPA spec should override the persistence unit values.
				//		- note that it is unclear in the spec whether JDBC value in the integration settings should override
				//			a JTA DataSource (nor the reverse).  However, that is a useful thing to support
				ConnectionProviderBuilder.getJpaConnectionProviderProperties( "db2" )
		);

		try {
			final Map<String, Object> properties = emf.getProperties();

			final Object hibernateJdbcDriver = properties.get( AvailableSettings.URL );
			assertThat( hibernateJdbcDriver, notNullValue() );

			final Object jpaJdbcDriver = properties.get( AvailableSettings.JPA_JDBC_URL );
			assertThat( (String) jpaJdbcDriver, containsString( "db2" ) );

			// see if the values had the affect to adjust the `ConnectionProvider` used
			final ConnectionProvider connectionProvider = emf.unwrap( SessionFactoryImplementor.class )
					.getServiceRegistry()
					.getService( ConnectionProvider.class );
			assertThat( connectionProvider, instanceOf( DriverManagerConnectionProviderImpl.class ) );
		}
		finally {
			emf.close();
		}
	}

	@Test
	@FailureExpected(
			jiraKey = "HHH-12858",
			message = "So it appears any use of the persistence.xml `jta-data-source` or `non-jta-data-source` " +
					"have precedence over integration settings, which is also incorrect"
	)
	public void testPassingIntegrationJpaDataSourceOverrideForJtaDataSourceElement() {
		final DataSource puDataSource = new DataSourceStub( "puDataSource" );
		final DataSource integrationDataSource = new DataSourceStub( "integrationDataSource" );

		PersistenceProvider provider = new HibernatePersistenceProvider() {
			@Override
			public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map integrationOverrides) {
				return super.createContainerEntityManagerFactory(
						new DelegatingPersistenceUnitInfo( info ) {
							@Override
							public DataSource getJtaDataSource() {
								// pretend the DataSource was defined using the `jta-data-source` element in persistence.xml
								//		- as opposed using `javax.persistence.jtaDataSource` under the `properties` element
								return puDataSource;
							}
						},
						integrationOverrides
				);
			}
		};

		final Map integrationOverrides = new HashMap();
		//noinspection unchecked
		integrationOverrides.put( AvailableSettings.JPA_JTA_DATASOURCE, integrationDataSource );

		final EntityManagerFactory emf = provider.createContainerEntityManagerFactory(
				new PersistenceUnitInfoAdapter(),
				integrationOverrides
		);

		try {
			final Map<String, Object> properties = emf.getProperties();

			final Object datasource = properties.get( AvailableSettings.JPA_JTA_DATASOURCE );
			assertThat( datasource, is( integrationDataSource ) );

			// see if the values had the affect to adjust the `ConnectionProvider` used
			final ConnectionProvider connectionProvider = emf.unwrap( SessionFactoryImplementor.class )
					.getServiceRegistry()
					.getService( ConnectionProvider.class );
			assertThat( connectionProvider, instanceOf( DatasourceConnectionProviderImpl.class ) );

			final DatasourceConnectionProviderImpl datasourceConnectionProvider = (DatasourceConnectionProviderImpl) connectionProvider;
			assertThat( datasourceConnectionProvider.getDataSource(), is( integrationDataSource ) );
		}
		finally {
			emf.close();
		}
	}

	@Test
	@FailureExpected( jiraKey = "HHH-12858", message = "regression - fix" )
	public void testIntegrationOverridesOfPersistenceXmlDataSource() {

		// mimics a DataSource defined in the persistence.xml
		final DataSourceStub dataSource = new DataSourceStub( "puDataSource" );
		final PersistenceUnitInfoAdapter info = new PersistenceUnitInfoAdapter() {

			@Override
			public DataSource getNonJtaDataSource() {
				return dataSource;
			}
		};


		// Now create "integration Map" that overrides the DataSource to use
		final DataSource override = new DataSourceStub( "integrationDataSource" );
		final Map<String,Object> integrationSettings = new HashMap<>();
		integrationSettings.put( AvailableSettings.JPA_NON_JTA_DATASOURCE, override );

		final PersistenceProvider provider = new HibernatePersistenceProvider();

		final EntityManagerFactory emf = provider.createContainerEntityManagerFactory(
				info,
				integrationSettings
		);

		try {
			final Map<String, Object> properties = emf.getProperties();

			assertThat( properties.get( AvailableSettings.JPA_NON_JTA_DATASOURCE ), notNullValue() );
			assertThat( properties.get( AvailableSettings.JPA_NON_JTA_DATASOURCE ), is( override ) );

			final SessionFactoryImplementor sessionFactory = emf.unwrap( SessionFactoryImplementor.class );
			final ConnectionProvider connectionProvider = sessionFactory.getServiceRegistry().getService( ConnectionProvider.class );
			assertThat( connectionProvider, instanceOf( DatasourceConnectionProviderImpl.class ) );

			final DatasourceConnectionProviderImpl dsProvider = (DatasourceConnectionProviderImpl) connectionProvider;
			assertThat( dsProvider.getDataSource(), is( override ) );
		}
		finally {
			emf.close();
		}
	}

	@Test
	@FailureExpected( jiraKey = "HHH-12858", message = "regression - fix" )
	public void testIntegrationOverridesOfPersistenceXmlDataSourceWithDriverManagerInfo() {

		// mimics a DataSource defined in the persistence.xml
		final DataSourceStub dataSource = new DataSourceStub( "puDataSource" );
		final PersistenceUnitInfoAdapter info = new PersistenceUnitInfoAdapter() {

			@Override
			public DataSource getNonJtaDataSource() {
				return dataSource;
			}
		};

		final Map<String,Object> integrationSettings = new HashMap<>();
		integrationSettings.put( AvailableSettings.JPA_JDBC_DRIVER, ConnectionProviderBuilder.DRIVER );
		integrationSettings.put( AvailableSettings.JPA_JDBC_URL, ConnectionProviderBuilder.URL );
		integrationSettings.put( AvailableSettings.JPA_JDBC_USER, ConnectionProviderBuilder.USER );
		integrationSettings.put( AvailableSettings.JPA_JDBC_PASSWORD, ConnectionProviderBuilder.PASS );

		final PersistenceProvider provider = new HibernatePersistenceProvider();

		final EntityManagerFactory emf = provider.createContainerEntityManagerFactory(
				info,
				integrationSettings
		);

		final SessionFactoryImplementor sessionFactory = emf.unwrap( SessionFactoryImplementor.class );
		final ConnectionProvider connectionProvider = sessionFactory.getServiceRegistry().getService( ConnectionProvider.class );
		assertThat( connectionProvider, instanceOf( DriverManagerConnectionProviderImpl.class ) );
	}

}
