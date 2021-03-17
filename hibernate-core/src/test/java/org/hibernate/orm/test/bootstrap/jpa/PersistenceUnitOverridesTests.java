/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bootstrap.jpa;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.metamodel.EntityType;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.testing.jdbc.DataSourceStub;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.jpa.DelegatingPersistenceUnitInfo;
import org.hibernate.testing.util.jpa.PersistenceUnitInfoAdapter;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class PersistenceUnitOverridesTests extends BaseUnitTestCase {

	@Test
	public void testPassingIntegrationJpaJdbcOverrides() {

		// the integration overrides say to use the "db2" JPA connection settings (which should override the persistence unit values)
		final Map integrationOverrides = ConnectionProviderBuilder.getJpaConnectionProviderProperties( "db2" );

		final EntityManagerFactory emf = new HibernatePersistenceProvider().createContainerEntityManagerFactory(
				new PersistenceUnitInfoAdapter() {
					@Override
					public Properties getProperties() {
						// effectively, the `persistence.xml` defines `db1` as the connection settings
						return ConnectionProviderBuilder.getJpaConnectionProviderProperties( "db1" );
					}
				},
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
	public void testPassingIntegrationJtaDataSourceOverrideForJpaJdbcSettings() {
		final PersistenceUnitInfoAdapter puInfo = new PersistenceUnitInfoAdapter(
				ConnectionProviderBuilder.getJpaConnectionProviderProperties( "db2" )
		);

		final DataSource integrationDataSource = new DataSourceStub( "integrationDataSource" );

		final HibernatePersistenceProvider provider = new HibernatePersistenceProvider();
		puInfo.getProperties().setProperty( AvailableSettings.HQL_BULK_ID_STRATEGY, MultiTableBulkIdStrategyStub.class.getName() );

		final EntityManagerFactory emf = provider.createContainerEntityManagerFactory(
				puInfo,
				Collections.singletonMap( AvailableSettings.JPA_JTA_DATASOURCE, integrationDataSource )
		);

		try {
			// first let's check the DataSource used in the EMF...
			final ConnectionProvider connectionProvider = emf.unwrap( SessionFactoryImplementor.class )
					.getServiceRegistry()
					.getService( ConnectionProvider.class );
			assertThat( connectionProvider, instanceOf( DatasourceConnectionProviderImpl.class ) );
			final DatasourceConnectionProviderImpl dsCp = (DatasourceConnectionProviderImpl) connectionProvider;
			assertThat( dsCp.getDataSource(), is( integrationDataSource ) );

			// now let's check that it is exposed via the EMF properties
			//		- note : the spec does not indicate that this should work, but
			//			it worked this way in previous versions
			final Object jtaDs = emf.getProperties().get( AvailableSettings.JPA_JTA_DATASOURCE );
			assertThat( jtaDs, is( integrationDataSource ) );

			// Additionally, we should have set Hibernate's DATASOURCE setting
			final Object hibDs = emf.getProperties().get( AvailableSettings.JPA_JTA_DATASOURCE );
			assertThat( hibDs, is( integrationDataSource ) );

			// Make sure the non-jta-data-source setting was cleared or otherwise null
			final Object nonJtaDs = emf.getProperties().get( AvailableSettings.JPA_NON_JTA_DATASOURCE );
			assertThat( nonJtaDs, nullValue() );
		}
		finally {
			emf.close();
		}
	}

	@Test
	public void testPassingIntegrationJpaJdbcOverrideForJtaDataSourceProperty() {
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
							public DataSource getJtaDataSource() {
								return null;
							}

							@Override
							public DataSource getNonJtaDataSource() {
								return null;
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
//	@FailureExpected(
//			jiraKey = "HHH-12858",
//			message = "Even though the JDBC settings override a DataSource *property*, it" +
//					" does not override a DataSource defined using the dedicated persistence.xml element"
//	)
	public void testPassingIntegrationJpaJdbcOverridesForJtaDataSourceElement() {
		PersistenceProvider provider = new HibernatePersistenceProvider() {
			@Override
			public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map integrationOverrides) {
				return super.createContainerEntityManagerFactory(
						new DelegatingPersistenceUnitInfo( info ) {
							// inject a JPA JTA DataSource setting into the PU
							final DataSource puDataSource = new DataSourceStub( "puDataSource" );

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
//	@FailureExpected(
//			jiraKey = "HHH-12858",
//			message = "So it appears any use of the persistence.xml `jta-data-source` or `non-jta-data-source` " +
//					"have precedence over integration settings, which is also incorrect"
//	)
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
		integrationOverrides.put( AvailableSettings.HQL_BULK_ID_STRATEGY, new MultiTableBulkIdStrategyStub() );

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
	@TestForIssue( jiraKey = "HHH-13640" )
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
		integrationSettings.put( AvailableSettings.HQL_BULK_ID_STRATEGY, new MultiTableBulkIdStrategyStub() );

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
	@TestForIssue( jiraKey = "HHH-13640" )
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

		try {
			final SessionFactoryImplementor sessionFactory = emf.unwrap( SessionFactoryImplementor.class );
			final ConnectionProvider connectionProvider = sessionFactory.getServiceRegistry().getService(
					ConnectionProvider.class );
			assertThat( connectionProvider, instanceOf( DriverManagerConnectionProviderImpl.class ) );
		}
		finally {
			emf.close();
		}
	}

	@Test
	public void testCfgXmlBaseline() {
		final PersistenceUnitInfoAdapter info = new PersistenceUnitInfoAdapter() {
			private final Properties props = new Properties();
			{
				props.put( org.hibernate.jpa.AvailableSettings.CFG_FILE, "org/hibernate/orm/test/bootstrap/jpa/hibernate.cfg.xml" );
			}

			@Override
			public Properties getProperties() {
				return props;
			}
		};

		final PersistenceProvider provider = new HibernatePersistenceProvider();

		final Map integrationSettings = Collections.emptyMap();

		final EntityManagerFactory emf = provider.createContainerEntityManagerFactory(
				info,
				integrationSettings
		);

		try {
			assertThat(
					emf.getProperties().get( AvailableSettings.DIALECT ),
					is( PersistenceUnitDialect.class.getName() )
			);
			assertThat(
					emf.unwrap( SessionFactoryImplementor.class ).getJdbcServices().getDialect(),
					instanceOf( PersistenceUnitDialect.class )
			);

			final EntityType<MappedEntity> entityMapping = emf.getMetamodel().entity( MappedEntity.class );
			assertThat( entityMapping, notNullValue() );
		}
		finally {
			emf.close();
		}
	}

	@Test
	public void testIntegrationOverridesOfCfgXml() {
		final PersistenceUnitInfoAdapter info = new PersistenceUnitInfoAdapter() {
			private final Properties props = new Properties();
			{
				props.put( org.hibernate.jpa.AvailableSettings.CFG_FILE, "org/hibernate/orm/test/bootstrap/jpa/hibernate.cfg.xml" );
			}

			@Override
			public Properties getProperties() {
				return props;
			}
		};

		final PersistenceProvider provider = new HibernatePersistenceProvider();

		final Map integrationSettings = Collections.singletonMap(
				AvailableSettings.DIALECT,
				IntegrationDialect.class.getName()
		);

		final EntityManagerFactory emf = provider.createContainerEntityManagerFactory(
				info,
				integrationSettings
		);

		try {
			assertThat(
					emf.getProperties().get( AvailableSettings.DIALECT ),
					is( IntegrationDialect.class.getName() )
			);
			assertThat(
					emf.unwrap( SessionFactoryImplementor.class ).getJdbcServices().getDialect(),
					instanceOf( IntegrationDialect.class )
			);

			final EntityPersister entityMapping = emf.unwrap( SessionFactoryImplementor.class )
					.getMetamodel()
					.entityPersister( MappedEntity.class );
			assertThat( entityMapping, notNullValue() );
			assertThat(
					entityMapping.getCacheAccessStrategy().getAccessType(),
					is( AccessType.READ_ONLY )
			);
		}
		finally {
			emf.close();
		}
	}

	public static class PersistenceUnitDialect extends Dialect {
	}

	@SuppressWarnings("WeakerAccess")
	public static class IntegrationDialect extends Dialect {
	}

	@Entity
	public static class MappedEntity {
		private Integer id;
		private String name;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class MultiTableBulkIdStrategyStub implements MultiTableBulkIdStrategy {

		@Override
		public void prepare(
				JdbcServices jdbcServices,
				JdbcConnectionAccess connectionAccess,
				MetadataImplementor metadata,
				SessionFactoryOptions sessionFactoryOptions) {

		}

		@Override
		public void release(
				JdbcServices jdbcServices, JdbcConnectionAccess connectionAccess) {

		}

		@Override
		public UpdateHandler buildUpdateHandler(
				SessionFactoryImplementor factory, HqlSqlWalker walker) {
			return null;
		}

		@Override
		public DeleteHandler buildDeleteHandler(
				SessionFactoryImplementor factory, HqlSqlWalker walker) {
			return null;
		}
	}
}
