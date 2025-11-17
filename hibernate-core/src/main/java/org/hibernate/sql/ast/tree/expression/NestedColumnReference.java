/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.from.EmbeddableFunctionTableReference;

/**
 * Model a column which is relative to a base expression e.g. {@code array[1].columnName}.
 * This is needed to model column references within e.g. arrays.
 */
public class NestedColumnReference extends ColumnReference {
	private final Expression baseExpression;

	public NestedColumnReference(EmbeddableFunctionTableReference tableReference, SelectableMapping selectableMapping) {
		super( tableReference, selectableMapping );
		this.baseExpression = tableReference.getExpression();
	}

	public Expression getBaseExpression() {
		return baseExpression;
	}

	@Override
	public String getReadExpression() {
		return super.getReadExpression();
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitNestedColumnReference( this );
	}
}
