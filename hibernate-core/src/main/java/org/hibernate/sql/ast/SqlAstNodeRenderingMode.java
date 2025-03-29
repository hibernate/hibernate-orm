/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast;

import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * The rendering mode to use for {@link SqlAstNode}.
 *
 * Some functions/contexts require the use of literals/expressions rather than parameters
 * like for example the `char` function in Derby which requires the length as literal.
 *
 * Another example is a function that renders a function argument into a subquery select and group by item.
 * It can use {@link #INLINE_PARAMETERS} so that a database can match such a select item to a group by item.
 * Without this, such queries would result in a query error.
 *
 * @author Christian Beikov
 * @see SqlAstTranslator#render(SqlAstNode, SqlAstNodeRenderingMode)
 */
public enum SqlAstNodeRenderingMode {
	/**
	 * Render node as is.
	 */
	DEFAULT,

	/**
	 * Render parameters as literals.
	 * All parameters within the {@link SqlAstNode} are rendered as literals.
	 */
	INLINE_PARAMETERS,

	/**
	 * Render all nested parameters as literals.
	 * All parameters within the {@link SqlAstNode} are rendered as literals.
	 */
	INLINE_ALL_PARAMETERS,

	/**
	 * Don't render plain parameters. Render it as literal or as expression.
	 * If the {@link SqlAstNode} to render is a parameter,
	 * it will be rendered either as literal or wrapped into a semantically equivalent expression
	 * such that it doesn't appear as plain parameter.
	 */
	NO_PLAIN_PARAMETER,

	/**
	 * Don't render untyped expressions e.g. plain parameters or <code>null</code> literals. Render it as literal or as expression.
	 * If the {@link SqlAstNode} to render is a parameter,
	 * it will be rendered either as literal or wrapped into a semantically equivalent expression
	 * such that it doesn't appear as plain parameter.
	 * <code>null</code> literals will be wrapped in a cast.
	 */
	NO_UNTYPED,

	/**
	 * Wrap all nested parameters with a database specific wrapping strategy,
	 * defaulting to wrapping via a subquery e.g. {@code (select ?)}.
	 * This is useful for certain databases that don't support parameters directly within certain functions, like Informix.
	 */
	WRAP_ALL_PARAMETERS
}
