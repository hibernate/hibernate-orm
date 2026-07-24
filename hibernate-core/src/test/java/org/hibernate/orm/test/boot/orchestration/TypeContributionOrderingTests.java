/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.orchestration;

import java.sql.Types;
import java.util.List;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.pipeline.internal.TypeContributionCoordinator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.spi.JpaSettings;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 * @since 9.0
 */
@BootstrapServiceRegistry(
		javaServices = @BootstrapServiceRegistry.JavaService(
				role = TypeContributor.class,
				impl = TypeContributionOrderingTests.ServiceLoadedTypeContributor.class
		)
)
@ServiceRegistry
public class TypeContributionOrderingTests {
	@Test
	void programmaticContributorOverridesDialect(ServiceRegistryScope registryScope) {
		final var descriptor = new NamedVarcharJdbcType( "programmatic" );
		final var typeConfiguration = scopedTypeConfiguration( registryScope );

		TypeContributionCoordinator.contribute(
				typeContributions( typeConfiguration ),
				List.of( new JdbcTypeContributor( descriptor, 1000 ) ),
				registryScope.getRegistry()
		);

		assertThat( typeConfiguration.getJdbcTypeRegistry().findDescriptor( Types.VARCHAR ) )
				.isSameAs( descriptor );
	}

	@Test
	void programmaticContributorsAreAppliedByOrdinal(ServiceRegistryScope registryScope) {
		final var higherPrecedence = new NamedVarcharJdbcType( "higher" );
		final var lowerPrecedence = new NamedVarcharJdbcType( "lower" );
		final var typeConfiguration = scopedTypeConfiguration( registryScope );

		TypeContributionCoordinator.contribute(
				typeContributions( typeConfiguration ),
				List.of(
						new JdbcTypeContributor( higherPrecedence, 2000 ),
						new JdbcTypeContributor( lowerPrecedence, 1000 )
				),
				registryScope.getRegistry()
		);

		assertThat( typeConfiguration.getJdbcTypeRegistry().findDescriptor( Types.VARCHAR ) )
				.isSameAs( higherPrecedence );
	}

	@Test
	void equalOrdinalProgrammaticContributorOverridesServiceContributor(ServiceRegistryScope registryScope) {
		final var programmatic = new NamedVarcharJdbcType( "programmatic" );
		final var typeConfiguration = scopedTypeConfiguration( registryScope );

		TypeContributionCoordinator.contribute(
				typeContributions( typeConfiguration ),
				List.of( new JdbcTypeContributor( programmatic, 1000 ) ),
				registryScope.getRegistry()
		);

		assertThat( typeConfiguration.getJdbcTypeRegistry().findDescriptor( Types.VARCHAR ) )
				.isSameAs( programmatic );
	}

	@Test
	void equalOrdinalProgrammaticContributorsRetainRegistrationOrder(ServiceRegistryScope registryScope) {
		final var earlier = new NamedVarcharJdbcType( "earlier" );
		final var later = new NamedVarcharJdbcType( "later" );
		final var typeConfiguration = scopedTypeConfiguration( registryScope );

		TypeContributionCoordinator.contribute(
				typeContributions( typeConfiguration ),
				List.of(
						new JdbcTypeContributor( earlier, 1000 ),
						new JdbcTypeContributor( later, 1000 )
				),
				registryScope.getRegistry()
		);

		assertThat( typeConfiguration.getJdbcTypeRegistry().findDescriptor( Types.VARCHAR ) )
				.isSameAs( later );
	}

	@Test
	@SuppressWarnings("removal")
	void jpaTypeContributorSettingUsesTheCoordinator() {
		final var descriptor = new NamedVarcharJdbcType( "jpa-setting" );
		final TypeContributorList contributorList =
				() -> List.of( new JdbcTypeContributor( descriptor, 1000 ) );

		try (var sessionFactory = new HibernatePersistenceConfiguration( "type-contributor-setting" )
				.property( JpaSettings.TYPE_CONTRIBUTORS, contributorList )
				.createEntityManagerFactory()) {
			final var typeConfiguration =
					( (SessionFactoryImplementor) sessionFactory ).getTypeConfiguration();
			assertThat( typeConfiguration.getJdbcTypeRegistry().findDescriptor( Types.VARCHAR ) )
					.isSameAs( descriptor );
		}
	}

	private static TypeContributions typeContributions(TypeConfiguration typeConfiguration) {
		return () -> typeConfiguration;
	}

	private static TypeConfiguration scopedTypeConfiguration(ServiceRegistryScope registryScope) {
		return new MetadataBuildingContextTestingImpl( registryScope.getRegistry() )
				.getBootstrapContext()
				.getTypeConfiguration();
	}

	private record JdbcTypeContributor(NamedVarcharJdbcType descriptor, int ordinal) implements TypeContributor {
		@Override
		public void contribute(
				TypeContributions typeContributions,
				org.hibernate.service.ServiceRegistry serviceRegistry) {
			typeContributions.contributeJdbcType( descriptor );
		}
	}

	private static class NamedVarcharJdbcType extends VarcharJdbcType {
		private final String name;

		private NamedVarcharJdbcType(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public static class ServiceLoadedTypeContributor implements TypeContributor {
		@Override
		public void contribute(
				TypeContributions typeContributions,
				org.hibernate.service.ServiceRegistry serviceRegistry) {
			typeContributions.contributeJdbcType( new NamedVarcharJdbcType( "service" ) );
		}
	}
}
