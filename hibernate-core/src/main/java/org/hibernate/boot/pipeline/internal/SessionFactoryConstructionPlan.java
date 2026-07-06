/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.Objects;

import org.hibernate.boot.mapping.internal.model.BootBindingModel;
import org.hibernate.boot.pipeline.spi.ResolvedSessionFactorySettings;
import org.hibernate.boot.pipeline.spi.SessionFactoryConstructionIdentity;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.metamodel.spi.DelayedSessionFactoryReference;

/// Internal handoff for ORM's default SessionFactory construction path.
///
/// This keeps the public producer request small while allowing ORM's default
/// factory to receive resolved construction products as a single parameter.
///
/// @since 9.0
/// @author Steve Ebersole
public record SessionFactoryConstructionPlan(
		MetadataImplementor metadata,
		ResolvedSessionFactorySettings resolvedSettings,
		SessionFactoryConstructionIdentity identity,
		SessionFactoryOptions options,
		BootstrapContext bootstrapContext,
		BootBindingModel bootBindingModel,
		DelayedSessionFactoryReference sessionFactoryReference,
		SessionFactoryRuntimeComponents runtimeComponents,
		StandardServiceComponents standardServiceComponents) {

	public SessionFactoryConstructionPlan(
			MetadataImplementor metadata,
			ResolvedSessionFactorySettings resolvedSettings,
			SessionFactoryConstructionIdentity identity,
			SessionFactoryOptions options,
			BootstrapContext bootstrapContext,
			BootBindingModel bootBindingModel,
			SessionFactoryRuntimeComponents runtimeComponents) {
		this(
				metadata,
				resolvedSettings,
				identity,
				options,
				bootstrapContext,
				bootBindingModel,
				runtimeComponents,
				StandardServiceComponentsBuilder.build( options )
		);
	}

	public SessionFactoryConstructionPlan(
			MetadataImplementor metadata,
			ResolvedSessionFactorySettings resolvedSettings,
			SessionFactoryConstructionIdentity identity,
			SessionFactoryOptions options,
			BootstrapContext bootstrapContext,
			BootBindingModel bootBindingModel,
			SessionFactoryRuntimeComponents runtimeComponents,
			StandardServiceComponents standardServiceComponents) {
		this(
				metadata,
				resolvedSettings,
				identity,
				options,
				bootstrapContext,
				bootBindingModel,
				new DelayedSessionFactoryReference(),
				runtimeComponents,
				standardServiceComponents
		);
	}

	public SessionFactoryConstructionPlan {
		Objects.requireNonNull( metadata );
		Objects.requireNonNull( options );
		Objects.requireNonNull( bootstrapContext );
		Objects.requireNonNull( bootBindingModel );
		Objects.requireNonNull( sessionFactoryReference );
		Objects.requireNonNull( runtimeComponents );
		Objects.requireNonNull( standardServiceComponents );
	}
}
