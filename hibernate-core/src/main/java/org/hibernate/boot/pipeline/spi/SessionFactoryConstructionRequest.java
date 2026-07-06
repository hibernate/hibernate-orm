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
/// @apiNote [SessionFactoryOptions] is exposed as a transitional bridge while
/// SessionFactory construction moves toward resolved factory settings and
/// prepared runtime components.
///
/// @since 9.0
/// @author Steve Ebersole
public interface SessionFactoryConstructionRequest {
	/// Finalized ORM metadata used for runtime factory construction.
	MetadataImplementor getMetadata();

	/// Transitional view of resolved SessionFactory options.
	SessionFactoryOptions getOptions();

	/// Base service registry used by the factory build.
	ServiceRegistry getServiceRegistry();
}
