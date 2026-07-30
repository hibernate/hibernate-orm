/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.spi;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.service.ServiceRegistry;

/// Request passed to a [SessionFactoryProducer] for the final
/// SessionFactory construction step.
///
/// The options view is the compatibility boundary shared by construction from
/// both the resolved-settings pipeline and legacy/native metadata bootstrap.
/// It is not the mutable source of factory settings: a producer receives
/// finalized metadata and read-only options after all factory customization
/// has been applied.
///
/// @since 9.0
/// @author Steve Ebersole
public interface SessionFactoryConstructionRequest {
	/// Finalized ORM metadata used for runtime factory construction.
	MetadataImplementor getMetadata();

	/// Read-only compatibility view of the finalized factory settings.
	///
	/// For the resolved-settings pipeline this is a projection of
	/// [ResolvedSessionFactorySettings].  The same contract is also available
	/// when construction originates from the legacy/native metadata path,
	/// where no `ResolvedSessionFactorySettings` product exists.
	///
	/// The returned view is also the runtime options SPI retained by the
	/// completed [org.hibernate.engine.spi.SessionFactoryImplementor].
	SessionFactoryOptions getOptions();

	/// Base service registry used by the factory build.
	ServiceRegistry getServiceRegistry();
}
