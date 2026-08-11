/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import org.hibernate.Incubating;

/// Interprets a logical transition using one collection's semantics and mapping facts.
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
@FunctionalInterface
public interface CollectionMutationInterpreter {
	CollectionInterpretationProduction interpret(CollectionInterpretationContext context);

	static CollectionMutationInterpreter legacyCompatible() {
		return LegacyCompatibleCollectionMutationInterpreter.INSTANCE;
	}
}
