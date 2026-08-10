/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.Incubating;

/// Outcome of attempting collection-delta production.
///
/// Requesting initialization is ordinary control flow. Producers must not perform
/// initialization themselves.
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public sealed interface CollectionDeltaProduction {
	/// A successfully produced immutable delta.
	record Produced(@Nonnull CollectionDelta delta) implements CollectionDeltaProduction {
	}

	/// The producer cannot safely describe the delta without initialization.
	record InitializationRequired() implements CollectionDeltaProduction {
	}

	/// Creates a successful production outcome.
	static Produced produced(CollectionDelta delta) {
		return new Produced( delta );
	}

	/// Returns the shared initialization-required outcome.
	static InitializationRequired initializationRequired() {
		return InitializationRequiredHolder.INSTANCE;
	}

	final class InitializationRequiredHolder {
		private static final InitializationRequired INSTANCE = new InitializationRequired();

		private InitializationRequiredHolder() {
		}
	}
}
