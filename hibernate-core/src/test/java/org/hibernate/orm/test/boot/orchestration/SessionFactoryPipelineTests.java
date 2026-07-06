/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.orchestration;

import java.util.Map;

import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.pipeline.internal.source.ContributionDiscoveryContext;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.pipeline.internal.MappingResolutionPipeline;
import org.hibernate.boot.pipeline.internal.ResolvedMapping;
import org.hibernate.boot.pipeline.internal.SessionFactoryPipeline;
import org.hibernate.boot.pipeline.internal.ResolvedMappingImplementor;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.pipeline.internal.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.pipeline.internal.settings.ResolvedMappingSettings;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.orm.test.boot.models.source.SimpleEntity;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SchemaManagementAction;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class SessionFactoryPipelineTests {
	@Test
	void hibernatePersistenceConfigurationBuildsSessionFactory() {
		try (var sessionFactory = new HibernatePersistenceConfiguration( "test" )
				.property( JdbcSettings.URL, "jdbc:h2:mem:hibernate-persistence-configuration;DB_CLOSE_DELAY=-1" )
				.property( JdbcSettings.USER, "sa" )
				.property( JdbcSettings.PASS, "" )
				.managedClass( SimpleEntity.class )
				.createEntityManagerFactory()) {
			assertThat( ( (SessionFactoryImplementor) sessionFactory ).getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( SimpleEntity.class ) ).isNotNull();
		}
	}

	@Test
	void hibernatePersistenceConfigurationExportsSchema() {
		new HibernatePersistenceConfiguration( "test" )
				.property( JdbcSettings.URL, "jdbc:h2:mem:hibernate-persistence-configuration-schema;DB_CLOSE_DELAY=-1" )
				.property( JdbcSettings.USER, "sa" )
				.property( JdbcSettings.PASS, "" )
				.schemaManagementDatabaseAction( SchemaManagementAction.DROP_AND_CREATE )
				.managedClass( SimpleEntity.class )
				.exportSchema();
	}

	@Test
	void sessionFactoryBuildTargetIsDefined(ServiceRegistryScope registryScope) {
		final var persistenceConfiguration = new HibernatePersistenceConfiguration( "test" )
				.managedClass( SimpleEntity.class );
		final var bootstrapSettings = SettingsResolver.resolveBootstrapSettings( persistenceConfiguration, Map.of() );
		final var mappingSettings = SettingsResolver.resolveMappingSettings(
				bootstrapSettings,
				persistenceConfiguration.defaultToOneFetchType()
		);
		final var resolvedMapping = resolveMapping(
				registryScope,
				persistenceConfiguration,
				bootstrapSettings,
				mappingSettings
		);
		final var sessionFactorySettings = SettingsResolver.resolveSessionFactorySettings(
				bootstrapSettings,
				registryScope.getRegistry()
		);

		try (var sessionFactory = SessionFactoryPipeline.build(
				sessionFactorySettings,
				resolvedMapping,
				registryScope.getRegistry()
		)) {
			assertThat( sessionFactory ).isNotNull();
			assertThat( sessionFactory.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( SimpleEntity.class ) ).isNotNull();
		}
	}

	@Test
	@ServiceRegistry(settings = {
			@Setting(name = JdbcSettings.URL, value = "jdbc:h2:mem:session-factory-smoke;DB_CLOSE_DELAY=-1"),
			@Setting(name = JdbcSettings.USER, value = "sa"),
			@Setting(name = JdbcSettings.PASS, value = "")
	})
	void sessionFactorySupportsBasicPersistAndQuery(ServiceRegistryScope registryScope) {
		final var persistenceConfiguration = new HibernatePersistenceConfiguration( "test" )
				.managedClass( RuntimeSmokeEntity.class );
		final var bootstrapSettings = SettingsResolver.resolveBootstrapSettings( persistenceConfiguration, Map.of() );
		final var mappingSettings = SettingsResolver.resolveMappingSettings(
				bootstrapSettings,
				persistenceConfiguration.defaultToOneFetchType()
		);
		final var resolvedMapping = resolveMapping(
				registryScope,
				persistenceConfiguration,
				bootstrapSettings,
				mappingSettings
		);
		final var sessionFactorySettings = SettingsResolver.resolveSessionFactorySettings(
				bootstrapSettings,
				registryScope.getRegistry()
		);

		try (var sessionFactory = SessionFactoryPipeline.build(
				sessionFactorySettings,
				resolvedMapping,
				registryScope.getRegistry()
		)) {
			sessionFactory.inSession( (session) -> session.doWork( (connection) -> {
				try (var statement = connection.createStatement()) {
					statement.execute( "drop table if exists runtime_smoke_entity" );
					statement.execute(
							"create table runtime_smoke_entity (id integer not null, name varchar(255), primary key (id))"
					);
				}
			} ) );
			try {
				sessionFactory.inTransaction( (session) -> session.persist( new RuntimeSmokeEntity( 1, "first" ) ) );

				final String name = sessionFactory.fromTransaction( (session) ->
						session.createQuery(
								"select e.name from RuntimeSmokeEntity e where e.id = :id",
								String.class
						)
								.setParameter( "id", 1 )
								.getSingleResult()
				);

				assertThat( name ).isEqualTo( "first" );
			}
			finally {
				sessionFactory.inSession( (session) -> session.doWork( (connection) -> {
					try (var statement = connection.createStatement()) {
						statement.execute( "drop table if exists runtime_smoke_entity" );
					}
				} ) );
			}
		}
	}

	@Test
	@ServiceRegistry(settings = {
			@Setting(name = JdbcSettings.URL, value = "jdbc:h2:mem:native-metadata-session-factory;DB_CLOSE_DELAY=-1"),
			@Setting(name = JdbcSettings.USER, value = "sa"),
			@Setting(name = JdbcSettings.PASS, value = "")
	})
	void nativeMetadataBuildPreservesResolvedMappingForSessionFactoryBuild(ServiceRegistryScope registryScope) {
		final var metadata = MetadataBuildingTestHelper.buildMetadata(
				registryScope.getRegistry(),
				new MappingSources().addManagedClass( RuntimeSmokeEntity.class )
		);

		assertThat( metadata ).isInstanceOf( ResolvedMappingImplementor.class );
		final var resolvedMapping = ( (ResolvedMappingImplementor) metadata ).getResolvedMapping();
		assertThat( resolvedMapping.categorizedDomainModel() ).isNotNull();
		assertThat( resolvedMapping.bindingState() ).isNotNull();

		try (var sessionFactory = (SessionFactoryImplementor) org.hibernate.testing.orm.junit.SessionFactoryUtil.buildSessionFactory( metadata )) {
			assertThat( sessionFactory.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( RuntimeSmokeEntity.class ) ).isNotNull();
		}
	}

	private static ResolvedMapping resolveMapping(
			ServiceRegistryScope registryScope,
			HibernatePersistenceConfiguration persistenceConfiguration,
			ResolvedBootstrapSettings bootstrapSettings,
			ResolvedMappingSettings mappingSettings) {
		final var mappingSources = MappingSources.from(
				persistenceConfiguration,
				bootstrapSettings,
				mappingSettings,
				new ContributionDiscoveryContext( registryScope.getRegistry().requireService( ClassLoaderService.class ) )
		);
		return MappingResolutionPipeline.resolve(
				bootstrapSettings,
				mappingSettings,
				mappingSources,
				registryScope.getRegistry()
		);
	}

	@Entity(name = "RuntimeSmokeEntity")
	@Table(name = "runtime_smoke_entity")
	public static class RuntimeSmokeEntity {
		@Id
		private Integer id;

		@Basic
		private String name;

		protected RuntimeSmokeEntity() {
		}

		public RuntimeSmokeEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
