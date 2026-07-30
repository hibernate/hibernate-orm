/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;

import static java.util.Comparator.comparingInt;

/// Coordinates dialect, service-loaded, and programmatic type contributions.
///
/// @since 9.0
/// @author Steve Ebersole
public final class TypeContributionCoordinator {
	private TypeContributionCoordinator() {
	}

	public static void contribute(
			TypeContributions typeContributions,
			List<TypeContributor> programmaticContributors,
			ServiceRegistry serviceRegistry) {
		serviceRegistry.requireService( JdbcServices.class )
				.getDialect()
				.contribute( typeContributions, serviceRegistry );

		sortedTypeContributors( serviceRegistry, programmaticContributors )
				.forEach( contributor -> contributor.contribute( typeContributions, serviceRegistry ) );
	}

	private static List<TypeContributor> sortedTypeContributors(
			ServiceRegistry serviceRegistry,
			List<TypeContributor> programmaticContributors) {
		final var serviceContributors = serviceRegistry.requireService( ClassLoaderService.class )
				.loadJavaServices( TypeContributor.class );
		final List<TypeContributor> contributors = new ArrayList<>( serviceContributors );
		contributors.sort(
				comparingInt( TypeContributor::ordinal )
						.thenComparing( contributor -> contributor.getClass().getName() )
		);
		contributors.addAll( programmaticContributors );
		contributors.sort( comparingInt( TypeContributor::ordinal ) );
		return contributors;
	}
}
