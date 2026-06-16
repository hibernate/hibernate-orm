/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.Objects;

import org.hibernate.boot.pipeline.spi.ResolvedSessionFactorySettings;
import org.hibernate.boot.pipeline.spi.SessionFactoryConstructionIdentity;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;

/// Internal handoff for ORM's default SessionFactory construction path.
///
/// This keeps the public producer request small while allowing ORM's default
/// factory to receive resolved construction products as a single parameter.
///
/// @since 9.0
/// @author Steve Ebersole
public record SessionFactoryConstructionPreparation(
		MetadataImplementor metadata,
		ResolvedSessionFactorySettings resolvedSettings,
		SessionFactoryConstructionIdentity identity,
		SessionFactoryOptions options,
		BootstrapContext bootstrapContext,
		SessionFactoryRuntimePreparation runtimePreparation) {

	public SessionFactoryConstructionPreparation {
		Objects.requireNonNull( metadata );
		Objects.requireNonNull( options );
		Objects.requireNonNull( bootstrapContext );
		Objects.requireNonNull( runtimePreparation );
	}

	public static SessionFactoryConstructionPreparation prepare(SessionFactoryConstructionState state) {
		return new SessionFactoryConstructionPreparation(
				state.metadata(),
				state.resolvedSettings(),
				state.identity(),
				state.options(),
				state.bootstrapContext(),
				SessionFactoryRuntimePreparation.prepare(
						state.metadata(),
						state.resolvedSettings(),
						state.bootstrapContext()
				)
		);
	}

	public static SessionFactoryConstructionPreparation prepare(
			MetadataImplementor metadata,
			SessionFactoryOptions options,
			BootstrapContext bootstrapContext) {
		return new SessionFactoryConstructionPreparation(
				metadata,
				null,
				null,
				options,
				bootstrapContext,
				SessionFactoryRuntimePreparation.prepare( metadata, options, bootstrapContext )
		);
	}
}
