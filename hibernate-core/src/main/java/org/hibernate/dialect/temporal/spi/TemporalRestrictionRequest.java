/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporal.spi;

import org.hibernate.SPI;
import org.hibernate.temporal.TemporalTableStrategy;

import static org.hibernate.SPI.Role.USE;

/// Provides the ephemeral query facts needed to choose temporal column
/// restrictions without exposing query-engine state.
///
/// Do not retain this request after
/// [TemporalTableSupport#useTemporalRestriction(TemporalRestrictionRequest)].
///
/// @param strategy the configured temporal-table storage strategy
/// @param temporalIdentifierPresent whether the query selects a historical instant
/// @param instantChangesetIdentifier whether changeset identifiers represent instants
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record TemporalRestrictionRequest(
		TemporalTableStrategy strategy,
		boolean temporalIdentifierPresent,
		boolean instantChangesetIdentifier) {
	public TemporalRestrictionRequest {
		if ( strategy == null ) {
			throw new IllegalArgumentException( "Temporal table strategy must not be null" );
		}
	}
}
