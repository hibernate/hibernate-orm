/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import org.hibernate.Incubating;

/// A list position calculated from persisted size plus a relative adjustment.
///
/// @param persistedSize The one shared persisted-size handle for the mutation
/// @param relativePosition The position relative to persisted size
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public record DeferredCollectionPosition(
		PersistedCollectionSize persistedSize,
		int relativePosition) {

	/// Resolve the absolute list position.
	public int resolve() {
		return persistedSize.get() + relativePosition;
	}
}
