/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.Incubating;
import org.hibernate.action.queue.spi.CollectionTransition;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/// Runtime and mapping facts used to interpret a collection mutation.
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public record CollectionInterpretationContext(
		@Nonnull PersistentCollection<?> collection,
		@Nonnull CollectionPersister persister,
		@Nonnull CollectionTransition transition,
		@Nonnull CollectionBaseline baseline,
		boolean emptySnapshot,
		boolean removalSkipped,
		boolean semanticDeltaRequired,
		@Nonnull SharedSessionContractImplementor session) {
}
