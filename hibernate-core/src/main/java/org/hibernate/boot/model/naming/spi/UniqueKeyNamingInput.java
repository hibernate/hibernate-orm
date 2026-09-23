/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import java.util.List;
import jakarta.annotation.Nonnull;
import org.hibernate.SPI;
import static java.util.Objects.requireNonNull;

/// Settled dependencies for an implicitly named unique key.
/// Explicit names bypass this request; naming does not run for discarded inferred keys.
/// Inferred aggregate-collection keys retain legacy member-column dependencies pending
/// a separate decision about aggregate uniqueness; those members are not owner-table columns.
///
/// @param table Selected logical table identity and finalized physical name
/// @param columns Nonempty, immutable column-name pairs in resolved declaration order;
/// each pair retains the selected logical reference and finalized physical name.
/// Temporal period-start additions retain their legacy exclusion from naming dependencies
/// pending the temporal naming compatibility decision.
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record UniqueKeyNamingInput(
		@Nonnull NamedTableNamingInput table,
		@Nonnull List<NamingNamePair> columns) {
	public UniqueKeyNamingInput {
		requireNonNull( table, "table" );
		columns = List.copyOf( columns );
		if ( columns.isEmpty() ) {
			throw new IllegalArgumentException( "A unique key must contain columns" );
		}
	}
}
