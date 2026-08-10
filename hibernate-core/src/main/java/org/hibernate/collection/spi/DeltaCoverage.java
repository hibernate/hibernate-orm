/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import org.hibernate.Incubating;

/// Describes how completely a [CollectionDelta] covers its reference state.
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public enum DeltaCoverage {
	/// The changes completely describe the difference from the baseline.
	COMPLETE,

	/// Only explicitly requested changes are known, as with queued operations on
	/// an uninitialized collection.
	EXPLICIT_CHANGES_ONLY
}
