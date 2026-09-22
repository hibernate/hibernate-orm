/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import java.util.Optional;

import org.hibernate.SPI;
import org.hibernate.annotations.SoftDeleteType;

import static java.util.Objects.requireNonNull;

/// Immutable naming facts for an entity or collection soft-delete indicator.
/// Entity indicators have no attribute path; collection paths are nonempty and
/// relative to the mapped owning entity. For a hierarchy indicator, the owner is
/// the root entity receiving the indicator, not its annotation declarer.
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record SoftDeleteColumnNamingInput(
		EntityNamingInput owner,
		Kind kind,
		Optional<String> attributePath,
		TableNamingInput table,
		SoftDeleteType strategy) {
	public SoftDeleteColumnNamingInput {
		requireNonNull( owner, "owner" );
		requireNonNull( kind, "kind" );
		requireNonNull( attributePath, "attributePath" );
		requireNonNull( table, "table" );
		requireNonNull( strategy, "strategy" );
		if ( kind == Kind.ENTITY ? attributePath.isPresent()
				: attributePath.isEmpty() || attributePath.get().isEmpty() ) {
			throw new IllegalArgumentException( "Invalid attribute path for soft-delete target " + kind );
		}
	}

	/// The mapping receiving the indicator.
	public enum Kind { ENTITY, COLLECTION }
}
