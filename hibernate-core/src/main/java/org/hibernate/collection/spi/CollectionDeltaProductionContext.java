/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/// Inputs available to a [CollectionDeltaProducer].
///
/// @param collection The persistent collection wrapper
/// @param persister The collection mapping descriptor
/// @param baseline The comparison reference selected by shared mutation preparation
/// @param session The session performing preparation
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public record CollectionDeltaProductionContext(
		@Nonnull PersistentCollection<?> collection,
		@Nonnull CollectionPersister persister,
		@Nonnull CollectionBaseline baseline,
		@Nonnull SharedSessionContractImplementor session) {
}
