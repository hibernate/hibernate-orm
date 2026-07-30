/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.orchestration;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatementObserver;
import org.hibernate.boot.pipeline.internal.source.ContributionDiscoveryContext;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.pipeline.internal.MappingResolutionPipeline;
import org.hibernate.boot.pipeline.internal.ResolvedMapping;
import org.hibernate.boot.pipeline.internal.SessionFactoryPipeline;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.cfg.TransactionSettings;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for the explicit SessionFactory bootstrap pipeline.
 *
 * @author Steve Ebersole
 */
@ServiceRegistry(settings = {
		@Setting(name = JdbcSettings.URL, value = "jdbc:h2:mem:session-factory-pipeline;DB_CLOSE_DELAY=-1"),
		@Setting(name = JdbcSettings.USER, value = "sa"),
		@Setting(name = JdbcSettings.PASS, value = "")
})
class BootstrapPipelineTest {
	@Test
	void buildsSessionFactoryFromResolvedProducts(ServiceRegistryScope registryScope) {
		final var persistenceConfiguration = new HibernatePersistenceConfiguration( "test" )
				.managedClass( PipelineEntity.class );
		final var bootstrapSettings = SettingsResolver.resolveBootstrapSettings(
				persistenceConfiguration,
				Map.of( PersistenceSettings.PERSISTENCE_UNIT_NAME, "test" )
		);
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
			assertThat( sessionFactory ).isInstanceOf( SessionFactoryImpl.class );
			assertThat( sessionFactory.getName() ).isEqualTo( "test" );
			assertThat( sessionFactory.getJndiName() ).isNull();
			assertThat( sessionFactory.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( PipelineEntity.class ) ).isNotNull();
		}
	}

	@Test
	void constructionIdentityUsesResolvedFactoryNameAndJndiName(ServiceRegistryScope registryScope) {
		final var settings = Map.of(
				PersistenceSettings.SESSION_FACTORY_NAME, "pipeline-explicit-name",
				PersistenceSettings.SESSION_FACTORY_JNDI_NAME, "java:hibernate/pipeline-explicit-jndi"
		);
		final var persistenceConfiguration = new HibernatePersistenceConfiguration( "test" )
				.managedClass( PipelineEntity.class );
		final var bootstrapSettings = SettingsResolver.resolveBootstrapSettings( persistenceConfiguration, settings );
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
			assertThat( sessionFactory.getName() ).isEqualTo( "pipeline-explicit-name" );
			assertThat( sessionFactory.getJndiName() ).isEqualTo( "java:hibernate/pipeline-explicit-jndi" );
			assertThat( sessionFactory.getUuid() ).isNotBlank();
		}
	}

	@Test
	void defaultConstructionPreparationUsesResolvedSessionFactorySettings(ServiceRegistryScope registryScope) {
		final StatementObserver statementObserver = (sql, batchPosition) -> {
		};
		final var sessionFactoryObserver = new RecordingSessionFactoryObserver();
		final var persistenceConfiguration = new HibernatePersistenceConfiguration( "test" )
				.statementObserver( statementObserver )
				.managedClass( PipelineEntity.class );
		final var bootstrapSettings = SettingsResolver.resolveBootstrapSettings(
				persistenceConfiguration,
				Map.of( TransactionSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true" )
		);
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
				registryScope.getRegistry(),
				sessionFactoryObserver
		)) {
			assertThat( sessionFactory.getStatementObserver() ).isSameAs( statementObserver );
			assertThat( sessionFactory.getSessionFactoryOptions()
					.isInitializeLazyStateOutsideTransactionsEnabled() ).isTrue();
			assertThat( sessionFactoryObserver.created.get() ).isTrue();
			assertThat( sessionFactoryObserver.closed.get() ).isFalse();
		}

		assertThat( sessionFactoryObserver.closed.get() ).isTrue();
	}

	private static ResolvedMapping resolveMapping(
			ServiceRegistryScope registryScope,
			HibernatePersistenceConfiguration persistenceConfiguration,
			org.hibernate.boot.pipeline.internal.settings.ResolvedBootstrapSettings bootstrapSettings,
			org.hibernate.boot.pipeline.internal.settings.ResolvedMappingSettings mappingSettings) {
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

	@Entity(name = "PipelineEntity")
	@Table(name = "pipeline_entity")
	public static class PipelineEntity {
		@Id
		private Integer id;

		@Basic
		private String name;

		protected PipelineEntity() {
		}
	}

	private static class RecordingSessionFactoryObserver implements SessionFactoryObserver {
		private final AtomicBoolean created = new AtomicBoolean();
		private final AtomicBoolean closed = new AtomicBoolean();

		@Override
		public void sessionFactoryCreated(SessionFactory factory) {
			created.set( true );
		}

		@Override
		public void sessionFactoryClosed(SessionFactory factory) {
			closed.set( true );
		}
	}
}
