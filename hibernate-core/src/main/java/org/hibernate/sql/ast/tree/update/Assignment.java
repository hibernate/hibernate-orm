/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.update;

import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * @author Steve Ebersole
 */
public class Assignment implements SqlAstNode {
	private final ColumnReference columnReference;
	private final Expression assignedValue;

	public Assignment(ColumnReference columnReference, Expression assignedValue) {
		this.columnReference = columnReference;
		this.assignedValue = assignedValue;
	}

	/**
	 * The column being updated.
	 */
	public ColumnReference getColumnReference() {
		return columnReference;
	}

	public Expression getAssignedValue() {
		return assignedValue;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitAssignment( this );
	}
}
