/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.internal;

/// Identifier operations shared by Oracle array DDL and SQL references.
///
/// @author Steve Ebersole
public final class OracleArrayTypeNames {
	private OracleArrayTypeNames() {
	}

	/// Add a helper suffix to the final identifier, inside any enclosing quotes.
	/// The input is an already rendered, optionally qualified Oracle identifier.
	public static String helperName(String arrayTypeName, String suffix) {
		return arrayTypeName.endsWith( "\"" )
				? arrayTypeName.substring( 0, arrayTypeName.length() - 1 ) + suffix + '"'
				: arrayTypeName + suffix;
	}
}
