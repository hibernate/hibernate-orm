/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import org.hibernate.boot.mapping.internal.model.BootBindingModel;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
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
		return new SessionFactoryConstructionPlan(
				state.metadata(),
				state.resolvedSettings(),
				state.identity(),
				state.options(),
				state.bootstrapContext(),
				state.bootBindingModel(),
				new DelayedSessionFactoryReference(),
				buildRuntimeComponents( state ),
				StandardServiceComponentsBuilder.build( state.options() )
		);
	}

	private static SessionFactoryRuntimeComponents buildRuntimeComponents(SessionFactoryConstructionState state) {
		return state.hasResolvedSettings()
				? SessionFactoryRuntimeComponentsBuilder.build(
						state.metadata(),
						state.resolvedSettings(),
						state.bootstrapContext()
				)
				: SessionFactoryRuntimeComponentsBuilder.build(
						state.metadata(),
						state.options(),
						state.bootstrapContext()
				);
	}

	public static SessionFactoryConstructionPlan build(
			MetadataImplementor metadata,
			SessionFactoryOptions options,
			BootstrapContext bootstrapContext,
			BootBindingModel bootBindingModel) {
		return new SessionFactoryConstructionPlan(
				metadata,
				null,
				null,
				options,
				bootstrapContext,
				bootBindingModel,
				new DelayedSessionFactoryReference(),
				SessionFactoryRuntimeComponentsBuilder.build( metadata, options, bootstrapContext ),
				StandardServiceComponentsBuilder.build( options )
		);
	}
}
