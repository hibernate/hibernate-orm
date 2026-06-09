/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.orchestration;

import java.util.Map;

import org.hibernate.boot.models.source.BootstrapSourceContributions;
import org.hibernate.boot.orchestration.MetadataResolver;
import org.hibernate.boot.orchestration.ResolvedMetadata;
import org.hibernate.boot.orchestration.SessionFactoryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.settings.BootstrapSettingsResolver;
import org.hibernate.boot.settings.SessionFactorySettingsResolver;
import org.hibernate.cfg.JdbcSettings;
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
class SessionFactoryBootstrapPipelineTest {
	@Test
	void buildsSessionFactoryFromResolvedProducts(ServiceRegistryScope registryScope) {
		final var persistenceConfiguration = new HibernatePersistenceConfiguration( "test" )
				.managedClass( PipelineEntity.class );
		final var bootstrapSettings = new BootstrapSettingsResolver()
				.resolve( persistenceConfiguration, Map.of() );
		final var resolvedMetadata = resolveMetadata(
				registryScope,
				persistenceConfiguration,
				bootstrapSettings
		);
		final var sessionFactorySettings = new SessionFactorySettingsResolver()
				.resolve( bootstrapSettings, registryScope.getRegistry() );

		try (var sessionFactory = new SessionFactoryBuilder().build(
				sessionFactorySettings,
				resolvedMetadata,
				registryScope.getRegistry()
		)) {
			assertThat( sessionFactory.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( PipelineEntity.class ) ).isNotNull();
		}
	}

	private static ResolvedMetadata resolveMetadata(
			ServiceRegistryScope registryScope,
			HibernatePersistenceConfiguration persistenceConfiguration,
			org.hibernate.boot.settings.ResolvedBootstrapSettings bootstrapSettings) {
		final var sourceContributions = BootstrapSourceContributions.from(
				persistenceConfiguration,
				bootstrapSettings,
				registryScope.getRegistry().requireService( ClassLoaderService.class )
		);
		return new MetadataResolver().resolve(
				bootstrapSettings,
				sourceContributions,
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
}
