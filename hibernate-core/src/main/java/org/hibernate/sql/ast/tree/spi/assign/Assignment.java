/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.assign;

import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.SingularAttributeReference;
import org.hibernate.sql.ast.tree.spi.SqlAstNode;

/**
 * @author Steve Ebersole
 */
public class Assignment implements SqlAstNode {
	private final SingularAttributeReference stateField;
	private final Expression assignedValue;

	public Assignment(SingularAttributeReference stateField, Expression assignedValue) {
		this.stateField = stateField;
		this.assignedValue = assignedValue;
	}

	public SingularAttributeReference getStateField() {
		return stateField;
	}

	public Expression getAssignedValue() {
		return assignedValue;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitAssignment( this );
	}
}
