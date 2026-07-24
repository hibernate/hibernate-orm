/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.io.Serial;
import java.io.Serializable;

import org.hibernate.Internal;

import static java.util.Objects.requireNonNull;

/// Stable identity for one source attribute declaration.
///
/// @since 9.0
/// @author Steve Ebersole
@Internal
public record DeclarationRole(String declaringTypeName, String attributeName) implements Serializable {
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
