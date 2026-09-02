/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

/// Selects where a derived table's column aliases are rendered.
///
/// @since 8.0
/// @author Steve Ebersole
public enum DerivedColumnAliasing {
	/// Render the column list after the identification variable.
	DECLARATION,
	/// Render aliases in the derived select list and only the identification variable outside it.
	SELECT_LIST,
	/// Render only the identification variable.
	IDENTIFICATION_VARIABLE_ONLY
}
