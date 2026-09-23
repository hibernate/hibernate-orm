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
/// All reference components are non-null.
///
/// @param owner The mapped entity receiving the indicator, or owning the collection; the root entity
/// for an entity hierarchy, not a mapped superclass declaring the annotation
/// @param kind Whether the indicator belongs to an entity or a collection
/// @param attributePath Empty for an entity indicator; a nonempty owner-relative collection path otherwise,
/// including embedded paths such as `details.items`. The Optional itself must not be null
/// @param table The actual destination table for the indicator, with settled dependency names
/// @param strategy The effective [org.hibernate.annotations.SoftDelete#strategy()], used by the default
/// naming implementation to select `active` or `deleted`
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
	public enum Kind {
		/// Indicator for an entity or entity hierarchy; no attribute path.
		ENTITY,
		/// Indicator for collection rows; requires an owner-relative collection path.
		COLLECTION
	}
}
