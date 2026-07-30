/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.spi;

import java.io.Serial;
import java.io.Serializable;

import static java.util.Objects.requireNonNull;

/// Stable identity for one source attribute declaration.
///
/// Declaration identity is independent of every concrete application of the
/// attribute. For example, an inherited mapped-superclass declaration has one
/// declaration role but may contribute applications with distinct
/// [MappingRole] values to several entities.
///
/// The string form is intended for diagnostics and archive rendering. Equality
/// is defined by the structured record components.
///
/// @since 9.0
/// @author Steve Ebersole
public record DeclarationRole(
		/// The name of the Java type which declares the persistent attribute.
		String declaringTypeName,
		/// The persistent attribute name.
		String attributeName)
		implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	public DeclarationRole {
		requireNonNull( declaringTypeName );
		requireNonNull( attributeName );
		if ( declaringTypeName.isBlank() || attributeName.isBlank() ) {
			throw new IllegalArgumentException( "Declaration role names cannot be blank" );
		}
	}

	@Override
	public String toString() {
		return "type:" + declaringTypeName + "#attribute:" + attributeName;
	}
}
