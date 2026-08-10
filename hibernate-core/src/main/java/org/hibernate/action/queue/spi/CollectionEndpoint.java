/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Incubating;
import org.hibernate.persister.collection.CollectionPersister;

/// Identifies one role/key endpoint of a collection transition.
///
/// @param persister The collection-role descriptor
/// @param key The collection key
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public record CollectionEndpoint(
		@Nonnull CollectionPersister persister,
		@Nullable Object key) {
}
