/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import java.util.Objects;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Immutable description of a synthetic table root required while building a
/// SQL AST query specification.
///
/// The table expression is rendered as a named table reference. The optional
/// identification variable is the complete alias text required by the
/// database, including any derived-column list.
///
/// @param tableExpression the non-blank table expression
/// @param identificationVariable the optional table alias
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record SyntheticTableGroupDescriptor(
		String tableExpression,
		@Nullable String identificationVariable) {
	public SyntheticTableGroupDescriptor {
		Objects.requireNonNull( tableExpression, "tableExpression" );
		if ( tableExpression.isBlank() ) {
			throw new IllegalArgumentException( "tableExpression must not be blank" );
		}
		if ( identificationVariable != null && identificationVariable.isBlank() ) {
			throw new IllegalArgumentException( "identificationVariable must not be blank" );
		}
	}
}
