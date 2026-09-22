/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import jakarta.annotation.Nonnull;
import org.hibernate.SPI;
import static java.util.Objects.requireNonNull;

/// One settled local-to-target foreign-key column pairing.
///
/// @param localColumn The local column's selected logical name and finalized physical name
/// @param referencedColumn The referenced column's selected logical name and finalized physical name
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record ForeignKeyColumnNamingInput(
		@Nonnull NamingNamePair localColumn,
		@Nonnull NamingNamePair referencedColumn) {
	public ForeignKeyColumnNamingInput {
		requireNonNull( localColumn, "localColumn" );
		requireNonNull( referencedColumn, "referencedColumn" );
	}
}
