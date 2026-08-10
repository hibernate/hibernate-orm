/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import org.hibernate.Incubating;

/// Identifies the provenance of facts represented by a [CollectionDelta].
///
/// Multiple sources may contribute to one delta.
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public enum DeltaSource {
	/// Changes discovered by comparing current state with stored loaded state.
	SNAPSHOT_COMPARISON,

	/// Changes discovered from an uninitialized collection's queued commands.
	QUEUED_OPERATION_LOG
}
