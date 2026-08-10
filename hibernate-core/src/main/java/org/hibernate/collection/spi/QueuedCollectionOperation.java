/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import jakarta.annotation.Nullable;

import org.hibernate.Incubating;

/// Immutable view of one command in an uninitialized collection's operation queue.
///
/// This is producer input, not an independently executable collection mutation.
/// Command order is significant, and a `CLEAR` command is an ordering barrier.
///
/// @param kind The command kind
/// @param addedValue The value added or assigned, if applicable
/// @param orphan The known previous or removed value, if applicable
/// @param position The map key or absolute list position, if known
/// @param order The command's zero-based position in the live queue
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public record QueuedCollectionOperation(
		Kind kind,
		@Nullable Object addedValue,
		@Nullable Object orphan,
		@Nullable Object position,
		int order) {

	public enum Kind {
		ADD,
		REMOVE,
		PUT,
		SET,
		CLEAR
	}
}
