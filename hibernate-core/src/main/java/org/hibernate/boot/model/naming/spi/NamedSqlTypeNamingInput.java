/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import java.util.Optional;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// The resolved name of a named SQL element dependency.
/// All reference components are non-null.
///
/// @param kind The element object kind
/// @param name The unqualified logical and physical object names
/// @param catalog The logical and physical catalog, if specified
/// @param schema The logical and physical schema, if specified
///
/// @since 9.0
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record NamedSqlTypeNamingInput(
		NamedSqlTypeKind kind,
		NamingNamePair name,
		Optional<NamingNamePair> catalog,
		Optional<NamingNamePair> schema) {
	public NamedSqlTypeNamingInput {
		requireNonNull( kind, "kind" );
		requireNonNull( name, "name" );
		requireNonNull( catalog, "catalog" );
		requireNonNull( schema, "schema" );
	}
}
