/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @since 7.0
 */
public class JsonValueEmptyBehavior implements SqlAstNode {
	public static final JsonValueEmptyBehavior NULL = new JsonValueEmptyBehavior( null );
	public static final JsonValueEmptyBehavior ERROR = new JsonValueEmptyBehavior( null );

	private final @Nullable Expression defaultExpression;

	private JsonValueEmptyBehavior(@Nullable Expression defaultExpression) {
		this.defaultExpression = defaultExpression;
	}

	public static JsonValueEmptyBehavior defaultOnEmpty(Expression defaultExpression) {
		return new JsonValueEmptyBehavior( defaultExpression );
	}

	public @Nullable Expression getDefaultExpression() {
		return defaultExpression;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		throw new UnsupportedOperationException("JsonValueEmptyBehavior doesn't support walking");
	}

}
