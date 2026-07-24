/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.orchestration;

import java.util.List;
import java.util.Map;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.pipeline.internal.FunctionRegistryCoordinator;
import org.hibernate.boot.pipeline.internal.FunctionRegistryCustomizations;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 * @since 9.0
 */
@BootstrapServiceRegistry(
		javaServices = @BootstrapServiceRegistry.JavaService(
				role = FunctionContributor.class,
				impl = FunctionContributionOrderingTests.ServiceLoadedContributor.class
		)
)
@ServiceRegistry
public class FunctionContributionOrderingTests {
	@Test
	void programmaticContributorOverridesDialect(ServiceRegistryScope registryScope) {
		final var contributor = new NamedFunctionContributor( "lower", "programmatic_lower", 1000 );
		final var functionRegistry = FunctionRegistryCoordinator.create();

		FunctionRegistryCoordinator.populate(
				functionRegistry,
				functionCustomizations( List.of( contributor ), Map.of() ),
				registryScope.getRegistry(),
				scopedTypeConfiguration( registryScope )
		);

		assertThat( functionRegistry.findFunctionDescriptor( "lower" ) )
				.isSameAs( contributor.contributedDescriptor );
	}

	@Test
	void programmaticContributorsAreAppliedByOrdinal(ServiceRegistryScope registryScope) {
		final var higherPrecedence = new NamedFunctionContributor( "stage1_ordered", "higher", 2000 );
		final var lowerPrecedence = new NamedFunctionContributor( "stage1_ordered", "lower", 1000 );
		final var functionRegistry = FunctionRegistryCoordinator.create();

		FunctionRegistryCoordinator.populate(
				functionRegistry,
				functionCustomizations(
						List.of( higherPrecedence, lowerPrecedence ),
						Map.of()
				),
				registryScope.getRegistry(),
				scopedTypeConfiguration( registryScope )
		);

		assertThat( functionRegistry.findFunctionDescriptor( "stage1_ordered" ) )
				.isSameAs( higherPrecedence.contributedDescriptor );
	}

	@Test
	void equalOrdinalProgrammaticContributorOverridesServiceContributor(ServiceRegistryScope registryScope) {
		final var programmatic = new NamedFunctionContributor( "stage1_equal", "programmatic", 1000 );
		final var functionRegistry = FunctionRegistryCoordinator.create();

		FunctionRegistryCoordinator.populate(
				functionRegistry,
				functionCustomizations( List.of( programmatic ), Map.of() ),
				registryScope.getRegistry(),
				scopedTypeConfiguration( registryScope )
		);

		assertThat( functionRegistry.findFunctionDescriptor( "stage1_equal" ) )
				.isSameAs( programmatic.contributedDescriptor );
	}

	@Test
	void equalOrdinalProgrammaticContributorsRetainRegistrationOrder(ServiceRegistryScope registryScope) {
		final var earlier = new NamedFunctionContributor( "stage1_registration_order", "earlier", 1000 );
		final var later = new NamedFunctionContributor( "stage1_registration_order", "later", 1000 );
		final var functionRegistry = FunctionRegistryCoordinator.create();

		FunctionRegistryCoordinator.populate(
				functionRegistry,
				functionCustomizations( List.of( earlier, later ), Map.of() ),
				registryScope.getRegistry(),
				scopedTypeConfiguration( registryScope )
		);

		assertThat( functionRegistry.findFunctionDescriptor( "stage1_registration_order" ) )
				.isSameAs( later.contributedDescriptor );
	}

	@Test
	void explicitSqlFunctionOverridesContributors(ServiceRegistryScope registryScope) {
		final var contributor = new NamedFunctionContributor( "stage1_explicit", "contributed", 2000 );
		final var descriptorSource = new SqmFunctionRegistry();
		final var explicitDescriptor = descriptorSource
				.namedDescriptorBuilder( "stage1_explicit", "explicit" )
				.descriptor();
		final var functionRegistry = FunctionRegistryCoordinator.create();

		FunctionRegistryCoordinator.populate(
				functionRegistry,
				functionCustomizations(
						List.of( contributor ),
						Map.of( "stage1_explicit", explicitDescriptor )
				),
				registryScope.getRegistry(),
				scopedTypeConfiguration( registryScope )
		);

		assertThat( functionRegistry.findFunctionDescriptor( "stage1_explicit" ) )
				.isSameAs( explicitDescriptor );
	}

	@Test
	void configurationCarriesFunctionCustomizationsSeparately() {
		final var contributor = new NamedFunctionContributor( "stage1_configuration", "configured", 1000 );

		try (var sessionFactory = new Configuration()
				.registerFunctionContributor( contributor )
				.buildSessionFactory()) {
			final var functionRegistry =
					( (SessionFactoryImplementor) sessionFactory ).getQueryEngine().getSqmFunctionRegistry();
			assertThat( functionRegistry.findFunctionDescriptor( "stage1_configuration" ) )
					.isSameAs( contributor.contributedDescriptor );
			assertThat( contributor.invocationCount ).isEqualTo( 1 );
		}
	}

	private static FunctionRegistryCustomizations functionCustomizations(
			List<FunctionContributor> contributors,
			Map<String, SqmFunctionDescriptor> explicitFunctions) {
		return new FunctionRegistryCustomizations( contributors, explicitFunctions );
	}

	private static TypeConfiguration scopedTypeConfiguration(ServiceRegistryScope registryScope) {
		return new MetadataBuildingContextTestingImpl( registryScope.getRegistry() )
				.getBootstrapContext()
				.getTypeConfiguration();
	}

	private static class NamedFunctionContributor implements FunctionContributor {
		private final String registrationName;
		private final String functionName;
		private final int ordinal;
		private SqmFunctionDescriptor contributedDescriptor;
		private int invocationCount;

		private NamedFunctionContributor(String registrationName, String functionName, int ordinal) {
			this.registrationName = registrationName;
			this.functionName = functionName;
			this.ordinal = ordinal;
		}

		@Override
		public void contributeFunctions(FunctionContributions functionContributions) {
			invocationCount++;
			contributedDescriptor = functionContributions.getFunctionRegistry()
					.namedDescriptorBuilder( registrationName, functionName )
					.register();
		}

		@Override
		public int ordinal() {
			return ordinal;
		}
	}

	public static class ServiceLoadedContributor implements FunctionContributor {
		@Override
		public void contributeFunctions(FunctionContributions functionContributions) {
			functionContributions.getFunctionRegistry()
					.namedDescriptorBuilder( "stage1_equal", "service" )
					.register();
		}
	}
}
