/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import org.hibernate.Incubating;

/// Produces an immutable semantic delta for one collection comparison state.
///
/// Implementations must not initialize the collection. If available state is
/// insufficient, return [CollectionDeltaProduction.InitializationRequired].
/// Producers may be shared and must not retain flush-specific state.
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
@FunctionalInterface
public interface CollectionDeltaProducer {
	/// Produce a delta or explicitly request coordinated initialization.
	CollectionDeltaProduction produceDelta(CollectionDeltaProductionContext context);

	/// Returns the shared conservative producer used by custom semantics.
	static CollectionDeltaProducer legacyCompatible() {
		return LegacyCompatibleCollectionDeltaProducer.INSTANCE;
	}
}
