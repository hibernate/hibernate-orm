/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Facts for a dependent table primary-key join; no association attribute is required.
///
/// All reference components are non-null.
///
/// @param table The dependent table receiving the new primary-key join column, such as a secondary or subclass table
/// @param reference The settled referenced table and ordered key columns, with the column for this decision selected
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record PrimaryKeyJoinColumnNamingInput(
		TableNamingInput table,
		ReferencedColumnsNamingInput reference) {
	public PrimaryKeyJoinColumnNamingInput {
		requireNonNull( table, "table" );
		requireNonNull( reference, "reference" );
	}
}
