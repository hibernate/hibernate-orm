/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.Incubating;

/// Outcome of interpreting a collection mutation.
///
/// Requesting initialization is ordinary control flow. Interpreters must not
/// initialize collection wrappers themselves.
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public sealed interface CollectionInterpretationProduction {
	record Produced(@Nonnull CollectionMutationInterpretation interpretation)
			implements CollectionInterpretationProduction {
	}

	record InitializationRequired() implements CollectionInterpretationProduction {
	}

	static Produced produced(CollectionMutationInterpretation interpretation) {
		return new Produced( interpretation );
	}

	static InitializationRequired initializationRequired() {
		return InitializationRequiredHolder.INSTANCE;
	}

	final class InitializationRequiredHolder {
		private static final InitializationRequired INSTANCE = new InitializationRequired();

		private InitializationRequiredHolder() {
		}
	}
}
