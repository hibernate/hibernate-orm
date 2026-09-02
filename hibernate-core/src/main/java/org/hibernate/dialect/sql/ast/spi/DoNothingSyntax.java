/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

/// Syntax used to express a do-nothing action in an on-duplicate-key clause.
///
/// @since 8.0
/// @author Steve Ebersole
public enum DoNothingSyntax {
	/// Assign the first inserted column to itself.
	SELF_ASSIGNMENT,

	/// Use the `nothing` keyword as the update action.
	NOTHING_KEYWORD
}
