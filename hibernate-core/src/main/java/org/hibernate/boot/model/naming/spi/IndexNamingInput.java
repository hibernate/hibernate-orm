/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import java.util.List;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Settled dependencies for an implicitly named index.
/// Explicit names bypass implicit naming. Unique declarations represented as UKs
/// instead use [UniqueKeyNamingInput].
///
/// @param table Selected logical table identity and finalized physical name
/// @param terms Nonempty immutable terms in declaration order, with resolved column names or opaque expressions
/// @param unique The source annotation's unique flag; type text is not interpreted to derive it
/// @param type Optional index type from [jakarta.persistence.Index#type()], or null when absent
/// @param using Optional indexing method from [jakarta.persistence.Index#using()], or null when absent
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record IndexNamingInput(
		@Nonnull NamedTableNamingInput table,
		@Nonnull List<IndexTermNamingInput> terms,
		boolean unique,
		@Nullable String type,
		@Nullable String using) {
	public IndexNamingInput {
		requireNonNull( table, "table" );
		terms = List.copyOf( terms );
		if ( terms.isEmpty() ) {
			throw new IllegalArgumentException( "An index must contain terms" );
		}
	}
}
