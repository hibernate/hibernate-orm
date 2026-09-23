/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;
import org.hibernate.relational.naming.spi.LogicalName;

import static java.util.Objects.requireNonNull;

/// A named table dependency with both naming stages.
///
/// All reference components are non-null.
///
/// @param names The non-null selected logical and finalized physical names of the same table, including their quoting
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record NamedTableNamingInput(NamingNamePair names) implements TableNamingInput {
	public NamedTableNamingInput {
		requireNonNull( names, "names" );
	}

	@Override
	public LogicalName logicalName() {
		return names.logicalName();
	}
}
