/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.relational.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Immutable database identifier comparison rules shared by a configured system.
/// Keys must be stable: equality, hashing, and ordering all use the same key.
/// Providers may refine comparison independently of the spelling required by JDBC.
///
/// @author Steve Ebersole
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT, SPI.Role.SUPPLY })
public interface IdentifierComparisonPolicy {
	/// Convert identifier text to the spelling expected by JDBC metadata lookup.
	String toDatabaseName(String text, boolean quoted);

	/// Produce the comparison key of a mapped physical name.
	default String comparisonKey(String text, boolean quoted) {
		return databaseNameComparisonKey( toDatabaseName( text, quoted ) );
	}

	/// Compare a physical name with spelling already obtained from JDBC metadata.
	/// The name and this policy belong to the same configured system.
	default boolean matchesDatabaseName(PhysicalName name, String databaseName) {
		requireNonNull( name, "name" );
		return comparisonKey( name.getText(), name.isQuoted() ).equals(
				databaseNameComparisonKey( requireNonNull( databaseName, "databaseName" ) ) );
	}

	/// Produce a key for spelling already obtained from JDBC metadata.
	/// Such spelling must not be folded again as an unquoted mapping name.
	default String databaseNameComparisonKey(String text) {
		return text;
	}
}
