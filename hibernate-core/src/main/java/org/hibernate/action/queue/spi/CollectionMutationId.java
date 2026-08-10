/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi;

import org.hibernate.Incubating;

/// Identifies one semantic collection mutation within a flush cycle.
///
/// Mutation identity is independent of SQL statement shape. Consequently,
/// operations from different collection mutations may share a JDBC batch while
/// retaining distinct completion accounting.
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public record CollectionMutationId(long value) {
	public CollectionMutationId {
		if ( value < 0 ) {
			throw new IllegalArgumentException( "Collection mutation identifiers must be non-negative" );
		}
	}
}
