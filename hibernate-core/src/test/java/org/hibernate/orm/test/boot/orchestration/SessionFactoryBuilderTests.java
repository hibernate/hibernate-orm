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
import org.hibernate.boot.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.settings.SessionFactorySettingsResolver;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.orm.test.boot.models.source.SimpleEntity;
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
 * @author Steve Ebersole
 */
@ServiceRegistry
public class SessionFactoryBuilderTests {
	@Test
	void sessionFactoryBuildTargetIsDefined(ServiceRegistryScope registryScope) {
		final var persistenceConfiguration = new HibernatePersistenceConfiguration( "test" )
				.managedClass( SimpleEntity.class );
		final var bootstrapSettings = BootstrapSettingsResolver.resolve( persistenceConfiguration, Map.of() );
		final var resolvedMetadata = resolveMetadata( registryScope, persistenceConfiguration, bootstrapSettings );
		final var sessionFactorySettings = SessionFactorySettingsResolver.resolve(
				bootstrapSettings,
				registryScope.getRegistry()
		);

		try (var sessionFactory = SessionFactoryBuilder.build(
				sessionFactorySettings,
				resolvedMetadata,
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
		final var bootstrapSettings = BootstrapSettingsResolver.resolve( persistenceConfiguration, Map.of() );
		final var resolvedMetadata = resolveMetadata( registryScope, persistenceConfiguration, bootstrapSettings );
		final var sessionFactorySettings = SessionFactorySettingsResolver.resolve(
				bootstrapSettings,
				registryScope.getRegistry()
		);

		try (var sessionFactory = SessionFactoryBuilder.build(
				sessionFactorySettings,
				resolvedMetadata,
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

	private static ResolvedMetadata resolveMetadata(
			ServiceRegistryScope registryScope,
			HibernatePersistenceConfiguration persistenceConfiguration,
			ResolvedBootstrapSettings bootstrapSettings) {
		final var sourceContributions = BootstrapSourceContributions.from(
				persistenceConfiguration,
				bootstrapSettings,
				registryScope.getRegistry().requireService( ClassLoaderService.class )
		);
		return MetadataResolver.resolve(
				bootstrapSettings,
				sourceContributions,
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
