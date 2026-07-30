/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import org.hibernate.boot.beanvalidation.BeanValidationPlan;
import org.hibernate.boot.pipeline.spi.ResolvedSessionFactorySettings;
import org.hibernate.boot.pipeline.spi.SessionFactoryConstructionIdentity;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.metamodel.internal.RuntimeMappingHandoff;
import org.hibernate.metamodel.spi.DelayedSessionFactoryReference;

/// Builds the constructor plan for ORM's default SessionFactory construction
/// path.
///
/// @since 9.0
/// @author Steve Ebersole
public final class SessionFactoryConstructionPlanBuilder {
	private SessionFactoryConstructionPlanBuilder() {
	}

	public static SessionFactoryConstructionPlan build(SessionFactoryConstructionState state) {
		return build( state, null );
	}

	public static SessionFactoryConstructionPlan build(
			MetadataImplementor metadata,
			SessionFactoryOptions options,
			BootstrapContext bootstrapContext,
			RuntimeMappingHandoff runtimeMappingHandoff) {
		return build( metadata, null, null, options, bootstrapContext, runtimeMappingHandoff, null );
	}

	public static SessionFactoryConstructionPlan build(
			MetadataImplementor metadata,
			ResolvedSessionFactorySettings resolvedSettings,
			SessionFactoryConstructionIdentity identity,
			SessionFactoryOptions options,
			BootstrapContext bootstrapContext,
			RuntimeMappingHandoff runtimeMappingHandoff,
			SessionFactoryRuntimeComponents suppliedRuntimeComponents) {
		final var state = new SessionFactoryConstructionState(
				metadata,
				resolvedSettings,
				identity,
				options,
				bootstrapContext,
				runtimeMappingHandoff
		);
		return build( state, suppliedRuntimeComponents );
	}

	private static SessionFactoryConstructionPlan build(
			SessionFactoryConstructionState state,
			SessionFactoryRuntimeComponents suppliedRuntimeComponents) {
		final var metadata = state.metadata();
		final var options = state.options();
		final var standardServiceComponents = StandardServiceComponentsBuilder.build( options );
		final var beanValidationPlan = BeanValidationPlan.prepare(
				metadata,
				options,
				standardServiceComponents.serviceRegistry()
		);
		beanValidationPlan.contributeRelationalConstraints();
		metadata.orderColumns( false );
		metadata.validate();
		final var runtimeComponents = suppliedRuntimeComponents == null
				? buildRuntimeComponents( state, standardServiceComponents )
				: suppliedRuntimeComponents;
		final var sessionFactoryReference = new DelayedSessionFactoryReference();
		return new SessionFactoryConstructionPlan(
				metadata,
				state.resolvedSettings(),
				state.identity(),
				options,
				state.runtimeMappingHandoff(),
				runtimeComponents,
				standardServiceComponents,
				sessionFactoryReference,
				beanValidationPlan,
				integratorLifecycle(
						metadata,
						runtimeComponents,
						standardServiceComponents,
						sessionFactoryReference
				)
		);
	}

	private static SessionFactoryRuntimeComponents buildRuntimeComponents(
			SessionFactoryConstructionState state,
			StandardServiceComponents standardServiceComponents) {
		return state.hasResolvedSettings()
				? SessionFactoryRuntimeComponentsBuilder.build(
						state.metadata(),
						state.resolvedSettings(),
						state.bootstrapContext(),
						standardServiceComponents.jdbcServices()
				)
				: SessionFactoryRuntimeComponentsBuilder.build(
						state.metadata(),
						state.options(),
						state.bootstrapContext(),
						standardServiceComponents.jdbcServices()
				);
	}

	private static SessionFactoryIntegratorLifecycle integratorLifecycle(
			MetadataImplementor metadata,
			SessionFactoryRuntimeComponents runtimeComponents,
			StandardServiceComponents standardServiceComponents,
			DelayedSessionFactoryReference sessionFactoryReference) {
		return new SessionFactoryIntegratorLifecycle(
				metadata,
				runtimeComponents.managedBeanRegistry(),
				sessionFactoryReference,
				standardServiceComponents.serviceRegistry()
		);
	}
}
