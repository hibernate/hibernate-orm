/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @since 7.0
 */
public class JsonValueErrorBehavior implements SqlAstNode {
	public static final JsonValueErrorBehavior NULL = new JsonValueErrorBehavior( null );
	public static final JsonValueErrorBehavior ERROR = new JsonValueErrorBehavior( null );

	private final @Nullable Expression defaultExpression;

	private JsonValueErrorBehavior(@Nullable Expression defaultExpression) {
		this.defaultExpression = defaultExpression;
	}

	public static JsonValueErrorBehavior defaultOnError(Expression defaultExpression) {
		return new JsonValueErrorBehavior( defaultExpression );
	}

	public @Nullable Expression getDefaultExpression() {
		return defaultExpression;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		throw new UnsupportedOperationException("JsonValueErrorBehavior doesn't support walking");
	}

}
