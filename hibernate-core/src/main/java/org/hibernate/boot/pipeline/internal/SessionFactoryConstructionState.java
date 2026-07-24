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
import org.hibernate.metamodel.internal.RuntimeMappingHandoff;

/// Internal state for ORM's default SessionFactory construction path.
/// This keeps resolved settings available to ORM while the public producer SPI
/// continues to expose only the transitional SessionFactoryOptions bridge.
///
/// @since 9.0
/// @author Steve Ebersole
record SessionFactoryConstructionState(
		MetadataImplementor metadata,
		ResolvedSessionFactorySettings resolvedSettings,
		SessionFactoryConstructionIdentity identity,
		SessionFactoryOptions options,
		BootstrapContext bootstrapContext,
		RuntimeMappingHandoff runtimeMappingHandoff) {

	SessionFactoryConstructionState {
		Objects.requireNonNull( metadata );
		Objects.requireNonNull( options );
		Objects.requireNonNull( bootstrapContext );
		Objects.requireNonNull( runtimeMappingHandoff );
	}

	static SessionFactoryConstructionState legacy(
			MetadataImplementor metadata,
			SessionFactoryOptions options,
			BootstrapContext bootstrapContext,
			RuntimeMappingHandoff runtimeMappingHandoff) {
		return new SessionFactoryConstructionState(
				metadata,
				null,
				null,
				options,
				bootstrapContext,
				runtimeMappingHandoff
		);
	}

	boolean hasResolvedSettings() {
		return resolvedSettings != null;
	}
}
