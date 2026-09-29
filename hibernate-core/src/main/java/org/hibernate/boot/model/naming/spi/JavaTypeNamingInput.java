/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import java.util.Optional;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// An immutable Java identity, usable without loading or reflecting on a class.
/// All reference components are non-null.
///
/// @param qualifiedName The Java binary name, including package and enclosing type
/// @param simpleName The actual Java simple name
/// @param arrayComponent The immediate Java array component; empty for a non-array type
///
/// @since 9.0
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record JavaTypeNamingInput(
		String qualifiedName,
		String simpleName,
		Optional<JavaTypeNamingInput> arrayComponent) {
	public JavaTypeNamingInput {
		requireNonNull( qualifiedName, "qualifiedName" );
		requireNonNull( simpleName, "simpleName" );
		requireNonNull( arrayComponent, "arrayComponent" );
	}
}
