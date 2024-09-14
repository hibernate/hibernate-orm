/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
