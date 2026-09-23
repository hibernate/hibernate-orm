/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Facts for naming a mapped table's primary-key constraint.
///
/// @param table The non-null table dependency, exposing its logical name and finalized
/// physical name with provenance and quoting; no column or table-role information is required
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record PrimaryKeyNamingInput(NamedTableNamingInput table) {
	public PrimaryKeyNamingInput {
		requireNonNull( table, "table" );
	}
}
