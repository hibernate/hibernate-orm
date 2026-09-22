/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import java.util.List;
import jakarta.annotation.Nonnull;
import org.hibernate.SPI;
import static java.util.Objects.requireNonNull;

/// Settled dependencies for an implicitly named foreign key.
/// Explicit constraint names bypass this request. Table dependencies may describe
/// inline views without inventing physical table names; this does not enable FK export.
///
/// @param table The selected local table identity and its physical name when present
/// @param referencedTable The selected target table identity and its physical name when present
/// @param referencesPrimaryKey Whether the resolved target columns form the target PK,
/// including an explicitly listed PK reference
/// @param columns Nonempty local/target pairs in settled mapping order, including actual
/// target columns for PK references; later DDL column reordering does not replay naming
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record ForeignKeyNamingInput(
		@Nonnull TableNamingInput table,
		@Nonnull TableNamingInput referencedTable,
		boolean referencesPrimaryKey,
		@Nonnull List<ForeignKeyColumnNamingInput> columns) {
	public ForeignKeyNamingInput {
		requireNonNull( table, "table" );
		requireNonNull( referencedTable, "referencedTable" );
		columns = List.copyOf( columns );
		if ( columns.isEmpty() ) {
			throw new IllegalArgumentException( "A foreign key must contain column pairs" );
		}
	}
}
