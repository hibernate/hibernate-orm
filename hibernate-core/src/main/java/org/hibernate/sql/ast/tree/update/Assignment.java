/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.update;

import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * @author Steve Ebersole
 */
public class Assignment implements SqlAstNode {
	private final Assignable assignable;
	private final Expression assignedValue;

	public Assignment(Assignable assignable, Expression assignedValue) {
		this.assignable = assignable;
		this.assignedValue = assignedValue;
	}

	/**
	 * The column being updated.
	 */
	public Assignable getAssignable() {
		return assignable;
	}

	public Expression getAssignedValue() {
		return assignedValue;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitAssignment( this );
	}
}
