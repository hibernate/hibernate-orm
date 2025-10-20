/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.SimpleDatabaseVersion;
import org.hibernate.engine.jdbc.connections.internal.DataSourceConnectionProvider;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.testing.jdbc.DataSourceStub;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.testing.util.jpa.DelegatingPersistenceUnitInfo;
import org.hibernate.testing.util.jpa.PersistenceUnitInfoAdapter;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
@BaseUnitTest
public class PersistenceUnitOverridesTests {

	@Test
	public void testPassingIntegrationJpaJdbcOverrides() {

		// the integration overrides say to use the "db2" JPA connection settings (which should override the persistence unit values)
		final Properties integrationOverrides = ConnectionProviderBuilder.getJpaConnectionProviderProperties( "db2" );

		try (final EntityManagerFactory emf = new HibernatePersistenceProvider().createContainerEntityManagerFactory(
				new PersistenceUnitInfoAdapter() {
					@Override
					public Properties getProperties() {
						// effectively, the `persistence.xml` defines `db1` as the connection settings
						return ConnectionProviderBuilder.getJpaConnectionProviderProperties( "db1" );
					}
				},
				integrationOverrides )) {

			final Map<String, Object> properties = emf.getProperties();

			final Object hibernateJdbcDriver = properties.get( AvailableSettings.JAKARTA_JDBC_URL );
			assertThat( hibernateJdbcDriver ).isNotNull();

			final Object jpaJdbcDriver = properties.get( AvailableSettings.JAKARTA_JDBC_URL );
			assertThat( (String) jpaJdbcDriver ).contains( "db2" );
		}
	}

	@Test
	public void testPassingIntegrationJtaDataSourceOverrideForJpaJdbcSettings() {
		final PersistenceUnitInfoAdapter puInfo = new PersistenceUnitInfoAdapter(
				ConnectionProviderBuilder.getJpaConnectionProviderProperties( "db2" )
		);

		final DataSource integrationDataSource = new DataSourceStub( "integrationDataSource" );

		final HibernatePersistenceProvider provider = new HibernatePersistenceProvider();
		// todo (6.0) : fix for Oracle see HHH-13432
//		puInfo.getProperties().setProperty( AvailableSettings.HQL_BULK_ID_STRATEGY, MultiTableBulkIdStrategyStub.class.getName() );

		try (final EntityManagerFactory emf = provider.createContainerEntityManagerFactory(
				puInfo,
				Collections.singletonMap( AvailableSettings.JAKARTA_JTA_DATASOURCE, integrationDataSource )
		)) {


			// first let's check the DataSource used in the EMF...
			final ConnectionProvider connectionProvider = emf.unwrap( SessionFactoryImplementor.class )
					.getServiceRegistry()
					.getService( ConnectionProvider.class );
			assertThat( connectionProvider ).isInstanceOf( DataSourceConnectionProvider.class );
			final DataSourceConnectionProvider dsCp = (DataSourceConnectionProvider) connectionProvider;
			assertThat( dsCp ).isNotNull();
			assertThat( dsCp.getDataSource() ).isEqualTo( integrationDataSource );

			// now let's check that it is exposed via the EMF properties
			//		- note : the spec does not indicate that this should work, but
			//			it worked this way in previous versions
			final Object jtaDs = emf.getProperties().get( AvailableSettings.JPA_JTA_DATASOURCE );
			assertThat( jtaDs ).isEqualTo( integrationDataSource );

			// Additionally, we should have set Hibernate's DATASOURCE setting
			final Object hibDs = emf.getProperties().get( AvailableSettings.JPA_JTA_DATASOURCE );
			assertThat( hibDs ).isEqualTo( integrationDataSource );

			// Make sure the non-jta-data-source setting was cleared or otherwise null
			final Object nonJtaDs = emf.getProperties().get( AvailableSettings.JAKARTA_NON_JTA_DATASOURCE );
			assertThat( nonJtaDs ).isNull();
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

		try (final EntityManagerFactory emf = provider.createContainerEntityManagerFactory(
				new PersistenceUnitInfoAdapter(),
				// however, provide JPA connection settings as "integration settings", which according to JPA spec should override the persistence unit values.
				//		- note that it is unclear in the spec whether JDBC value in the integration settings should override
				//			a JTA DataSource (nor the reverse).  However, that is a useful thing to support
				ConnectionProviderBuilder.getJpaConnectionProviderProperties( "db2" )
		)) {
			final Map<String, Object> properties = emf.getProperties();

			final Object hibernateJdbcDriver = properties.get( AvailableSettings.URL );
			assertThat( hibernateJdbcDriver ).isNotNull();

			final Object jpaJdbcDriver = properties.get( AvailableSettings.JPA_JDBC_URL );
			assertThat( (String) jpaJdbcDriver ).contains( "db2" );

			// see if the values had the affect to adjust the `ConnectionProvider` used
			final ConnectionProvider connectionProvider = emf.unwrap( SessionFactoryImplementor.class )
					.getServiceRegistry()
					.getService( ConnectionProvider.class );
			assertThat( connectionProvider ).isInstanceOf( DriverManagerConnectionProvider.class );
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

		try (final EntityManagerFactory emf = provider.createContainerEntityManagerFactory(
				new PersistenceUnitInfoAdapter(),
				// however, provide JPA connection settings as "integration settings", which according to JPA spec should override the persistence unit values.
				//		- note that it is unclear in the spec whether JDBC value in the integration settings should override
				//			a JTA DataSource (nor the reverse).  However, that is a useful thing to support
				ConnectionProviderBuilder.getJpaConnectionProviderProperties( "db2" )
		)) {

			final Map<String, Object> properties = emf.getProperties();

			final Object hibernateJdbcDriver = properties.get( AvailableSettings.URL );
			assertThat( hibernateJdbcDriver ).isNotNull();

			final Object jpaJdbcDriver = properties.get( AvailableSettings.JPA_JDBC_URL );
			assertThat( (String) jpaJdbcDriver ).contains( "db2" );

			// see if the values had the affect to adjust the `ConnectionProvider` used
			final ConnectionProvider connectionProvider = emf.unwrap( SessionFactoryImplementor.class )
					.getServiceRegistry()
					.getService( ConnectionProvider.class );
			assertThat( connectionProvider ).isInstanceOf( DriverManagerConnectionProvider.class );
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

		final Map<String, Object> integrationOverrides = new HashMap<>();
		//noinspection unchecked
		integrationOverrides.put( AvailableSettings.JPA_JTA_DATASOURCE, integrationDataSource );
		// todo (6.0) : fix for Oracle see HHH-13432
//		integrationOverrides.put( AvailableSettings.HQL_BULK_ID_STRATEGY, new MultiTableBulkIdStrategyStub() );

		try (final EntityManagerFactory emf = provider.createContainerEntityManagerFactory(
				new PersistenceUnitInfoAdapter(),
				integrationOverrides
		)) {
			final Map<String, Object> properties = emf.getProperties();

			final Object datasource = properties.get( AvailableSettings.JPA_JTA_DATASOURCE );
			assertThat( datasource ).isEqualTo( integrationDataSource );

			// see if the values had the affect to adjust the `ConnectionProvider` used
			final ConnectionProvider connectionProvider = emf.unwrap( SessionFactoryImplementor.class )
					.getServiceRegistry()
					.getService( ConnectionProvider.class );
			assertThat( connectionProvider ).isInstanceOf( DataSourceConnectionProvider.class );

			final DataSourceConnectionProvider datasourceConnectionProvider = (DataSourceConnectionProvider) connectionProvider;
			assertThat( datasourceConnectionProvider ).isNotNull();
			assertThat( datasourceConnectionProvider.getDataSource() ).isEqualTo( integrationDataSource );
		}
	}

	@Test
	@JiraKey(value = "HHH-13640")
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
		final Map<String, Object> integrationSettings = new HashMap<>();
		integrationSettings.put( AvailableSettings.JPA_NON_JTA_DATASOURCE, override );
		// todo (6.0) : fix for Oracle see HHH-13432
//		integrationSettings.put( AvailableSettings.HQL_BULK_ID_STRATEGY, new MultiTableBulkIdStrategyStub() );

		final PersistenceProvider provider = new HibernatePersistenceProvider();

		try (final EntityManagerFactory emf = provider.createContainerEntityManagerFactory( info,
				integrationSettings )) {
			final Map<String, Object> properties = emf.getProperties();

			assertThat( properties.get( AvailableSettings.JPA_NON_JTA_DATASOURCE ) ).isNotNull();
			assertThat( properties.get( AvailableSettings.JPA_NON_JTA_DATASOURCE ) ).isEqualTo( override );

			final SessionFactoryImplementor sessionFactory = emf.unwrap( SessionFactoryImplementor.class );
			final ConnectionProvider connectionProvider = sessionFactory.getServiceRegistry()
					.getService( ConnectionProvider.class );
			assertThat( connectionProvider ).isInstanceOf( DataSourceConnectionProvider.class );

			final DataSourceConnectionProvider dsProvider = (DataSourceConnectionProvider) connectionProvider;
			assertThat( dsProvider.getDataSource() ).isEqualTo( override );
		}
	}

	@Test
	@JiraKey(value = "HHH-13640")
	public void testIntegrationOverridesOfPersistenceXmlDataSourceWithDriverManagerInfo() {

		// mimics a DataSource defined in the persistence.xml
		final DataSourceStub dataSource = new DataSourceStub( "puDataSource" );
		final PersistenceUnitInfoAdapter info = new PersistenceUnitInfoAdapter() {

			@Override
			public DataSource getNonJtaDataSource() {
				return dataSource;
			}
		};

		final Map<String, Object> integrationSettings = ServiceRegistryUtil.createBaseSettings();
		integrationSettings.put( AvailableSettings.JPA_JDBC_DRIVER, ConnectionProviderBuilder.DRIVER );
		integrationSettings.put( AvailableSettings.JPA_JDBC_URL, ConnectionProviderBuilder.URL );
		integrationSettings.put( AvailableSettings.JPA_JDBC_USER, ConnectionProviderBuilder.USER );
		integrationSettings.put( AvailableSettings.JPA_JDBC_PASSWORD, ConnectionProviderBuilder.PASS );
		integrationSettings.put( DriverManagerConnectionProvider.INIT_SQL, "" );

		final PersistenceProvider provider = new HibernatePersistenceProvider();

		try (final EntityManagerFactory emf = provider.createContainerEntityManagerFactory(
				info,
				integrationSettings
		)) {
			final SessionFactoryImplementor sessionFactory = emf.unwrap( SessionFactoryImplementor.class );
			final ConnectionProvider connectionProvider = sessionFactory.getServiceRegistry().getService(
					ConnectionProvider.class );
			assertThat( connectionProvider ).isInstanceOf( DriverManagerConnectionProvider.class );
		}
	}

	@Test
	@JiraKey(value = "HHH-13640")
	public void testIntegrationOverridesOfPersistenceXmlDataSourceWithDriverManagerInfoUsingJakarta() {

		// mimics a DataSource defined in the persistence.xml
		final DataSourceStub dataSource = new DataSourceStub( "puDataSource" );
		final PersistenceUnitInfoAdapter info = new PersistenceUnitInfoAdapter() {

			@Override
			public DataSource getNonJtaDataSource() {
				return dataSource;
			}
		};

		final Map<String, Object> integrationSettings = ServiceRegistryUtil.createBaseSettings();
		integrationSettings.put( AvailableSettings.JAKARTA_JDBC_DRIVER, ConnectionProviderBuilder.DRIVER );
		integrationSettings.put( AvailableSettings.JAKARTA_JDBC_URL, ConnectionProviderBuilder.URL );
		integrationSettings.put( AvailableSettings.JAKARTA_JDBC_USER, ConnectionProviderBuilder.USER );
		integrationSettings.put( AvailableSettings.JAKARTA_JDBC_PASSWORD, ConnectionProviderBuilder.PASS );
		integrationSettings.put( DriverManagerConnectionProvider.INIT_SQL, "" );

		final PersistenceProvider provider = new HibernatePersistenceProvider();

		try (final EntityManagerFactory emf = provider.createContainerEntityManagerFactory(
				info,
				integrationSettings
		)) {
			final SessionFactoryImplementor sessionFactory = emf.unwrap( SessionFactoryImplementor.class );
			final ConnectionProvider connectionProvider = sessionFactory.getServiceRegistry().getService(
					ConnectionProvider.class );
			assertThat( connectionProvider ).isInstanceOf( DriverManagerConnectionProvider.class );
		}
	}

	@Test
	public void testCfgXmlBaseline() {
		final PersistenceUnitInfoAdapter info = new PersistenceUnitInfoAdapter() {
			private final Properties props = new Properties();

			{
				props.put( AvailableSettings.CFG_XML_FILE, "org/hibernate/orm/test/bootstrap/jpa/hibernate.cfg.xml" );
			}

			@Override
			public Properties getProperties() {
				return props;
			}
		};

		final PersistenceProvider provider = new HibernatePersistenceProvider();

		final Map<String, Object> integrationSettings = ServiceRegistryUtil.createBaseSettings();

		try (final EntityManagerFactory emf = provider.createContainerEntityManagerFactory(
				info,
				integrationSettings
		)) {
			assertThat( emf.getProperties().get( AvailableSettings.DIALECT ) )
					.isEqualTo( PersistenceUnitDialect.class.getName() );

			assertThat( emf.unwrap( SessionFactoryImplementor.class ).getJdbcServices().getDialect() )
					.isInstanceOf( PersistenceUnitDialect.class );

			assertThat( emf.getMetamodel().entity( MappedEntity.class ) )
					.isNotNull();
		}
	}

	@Test
	public void testIntegrationOverridesOfCfgXml() {
		final PersistenceUnitInfoAdapter info = new PersistenceUnitInfoAdapter() {
			private final Properties props = new Properties();

			{
				props.put( AvailableSettings.CFG_XML_FILE, "org/hibernate/orm/test/bootstrap/jpa/hibernate.cfg.xml" );
			}

			@Override
			public Properties getProperties() {
				return props;
			}
		};

		final PersistenceProvider provider = new HibernatePersistenceProvider();

		final Map<String, Object> integrationSettings = ServiceRegistryUtil.createBaseSettings();
		integrationSettings.put( AvailableSettings.DIALECT, IntegrationDialect.class.getName() );

		try (final EntityManagerFactory emf = provider.createContainerEntityManagerFactory(
				info,
				integrationSettings
		)) {
			assertThat( emf.getProperties().get( AvailableSettings.DIALECT ) )
					.isEqualTo( IntegrationDialect.class.getName() );

			assertThat( emf.unwrap( SessionFactoryImplementor.class ).getJdbcServices().getDialect() )
					.isInstanceOf( IntegrationDialect.class );

			final EntityPersister entityMapping = emf.unwrap( SessionFactoryImplementor.class )
					.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( MappedEntity.class );
			assertThat( entityMapping ).isNotNull();
			assertThat( entityMapping.getCacheAccessStrategy().getAccessType() )
					.isEqualTo( AccessType.READ_ONLY );
		}
	}

	public static class PersistenceUnitDialect extends Dialect {
		@Override
		public DatabaseVersion getVersion() {
			return SimpleDatabaseVersion.ZERO_VERSION;
		}
	}

	public static class IntegrationDialect extends Dialect {
		@Override
		public DatabaseVersion getVersion() {
			return SimpleDatabaseVersion.ZERO_VERSION;
		}
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

//	public static class MultiTableBulkIdStrategyStub implements MultiTableBulkIdStrategy {
//
//		@Override
//		public void prepare(
//				JdbcServices jdbcServices,
//				JdbcConnectionAccess connectionAccess,
//				MetadataImplementor metadata,
//				SessionFactoryOptions sessionFactoryOptions,
//				SqlStringGenerationContext sqlStringGenerationContext) {
//
//		}
//
//		@Override
//		public void release(
//				JdbcServices jdbcServices, JdbcConnectionAccess connectionAccess) {
//
//		}
//
//		@Override
//		public UpdateHandler buildUpdateHandler(
//				SessionFactoryImplementor factory, HqlSqlWalker walker) {
//			return null;
//		}
//
//		@Override
//		public DeleteHandler buildDeleteHandler(
//				SessionFactoryImplementor factory, HqlSqlWalker walker) {
//			return null;
//		}
//	}
}
