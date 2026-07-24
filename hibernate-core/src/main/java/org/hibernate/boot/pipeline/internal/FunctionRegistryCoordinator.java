/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.Comparator.comparingInt;

/// Coordinates creation and bootstrap-time population of the SQM function registry.
///
/// @since 9.0
public final class FunctionRegistryCoordinator {
	private FunctionRegistryCoordinator() {
	}

	public static SqmFunctionRegistry create() {
		return new SqmFunctionRegistry();
	}

	public static void populate(
			SqmFunctionRegistry functionRegistry,
			FunctionRegistryCustomizations functionCustomizations,
			ServiceRegistry serviceRegistry,
			TypeConfiguration typeConfiguration) {
		functionCustomizations = functionCustomizations == null
				? FunctionRegistryCustomizations.NONE
				: functionCustomizations;

		final var functionContributions = new FunctionContributionsImpl(
				serviceRegistry,
				typeConfiguration,
				functionRegistry
		);

		serviceRegistry.requireService( JdbcServices.class )
				.getDialect()
				.initializeFunctionRegistry( functionContributions );

		sortedFunctionContributors( serviceRegistry, functionCustomizations.functionContributors() )
				.forEach( contributor -> contributor.contributeFunctions( functionContributions ) );

		functionCustomizations.sqlFunctions().forEach( functionRegistry::register );
	}

	private static List<FunctionContributor> sortedFunctionContributors(
			ServiceRegistry serviceRegistry,
			List<FunctionContributor> programmaticContributors) {
		final var functionContributors =
				serviceRegistry.requireService( ClassLoaderService.class )
						.loadJavaServices( FunctionContributor.class );
		final List<FunctionContributor> contributors = new ArrayList<>( functionContributors );
		contributors.sort(
				comparingInt( FunctionContributor::ordinal )
						.thenComparing( a -> a.getClass().getName() )
		);
		contributors.addAll( programmaticContributors );
		contributors.sort( comparingInt( FunctionContributor::ordinal ) );
		return contributors;
	}

	private static class FunctionContributionsImpl implements FunctionContributions {
		private final ServiceRegistry serviceRegistry;
		private final TypeConfiguration typeConfiguration;
		private final SqmFunctionRegistry functionRegistry;

		private FunctionContributionsImpl(
				ServiceRegistry serviceRegistry,
				TypeConfiguration typeConfiguration,
				SqmFunctionRegistry functionRegistry) {
			this.serviceRegistry = serviceRegistry;
			this.typeConfiguration = typeConfiguration;
			this.functionRegistry = functionRegistry;
		}

		@Override
		public TypeConfiguration getTypeConfiguration() {
			return typeConfiguration;
		}

		@Override
		public SqmFunctionRegistry getFunctionRegistry() {
			return functionRegistry;
		}

		@Override
		public ServiceRegistry getServiceRegistry() {
			return serviceRegistry;
		}
	}
}
