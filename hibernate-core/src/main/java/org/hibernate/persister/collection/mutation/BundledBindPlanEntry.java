/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

/// Composition of the element of a collection along with its index relative to the iterator
/// returned from [org.hibernate.collection.spi.PersistentCollection#entries()].
/// Used in constructing "bundled operations".
///
/// @see org.hibernate.cfg.FlushSettings#BUNDLE_COLLECTION_OPERATIONS
///
/// @author Steve Ebersole
public record BundledBindPlanEntry(Object entry, int entryIndex) {
}
