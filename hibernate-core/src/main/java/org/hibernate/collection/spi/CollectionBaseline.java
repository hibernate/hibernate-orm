/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import org.hibernate.Incubating;

/// Identifies the reference state against which a [CollectionDelta] is expressed.
///
/// This is independent of how the changes were discovered, which is described by
/// [DeltaSource].
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public enum CollectionBaseline {
	/// A synthetic empty reference because no prior collection state exists.
	EMPTY,

	/// The stored loaded-state snapshot used as the comparison reference.
	LOADED,

	/// Existing collection state which has not been materialized.
	UNINITIALIZED
}
