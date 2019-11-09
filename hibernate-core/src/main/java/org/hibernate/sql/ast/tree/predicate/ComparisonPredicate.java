/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.predicate;

import org.hibernate.query.ComparisonOperator;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * @author Steve Ebersole
 */
public class ComparisonPredicate implements Predicate {
	private final Expression leftHandExpression;
	private ComparisonOperator operator;
	private final Expression rightHandExpression;

	public ComparisonPredicate(
			Expression leftHandExpression,
			ComparisonOperator operator,
			Expression rightHandExpression) {
		this.leftHandExpression = leftHandExpression;
		this.operator = operator;
		this.rightHandExpression = rightHandExpression;
	}

	public Expression getLeftHandExpression() {
		return leftHandExpression;
	}

	public Expression getRightHandExpression() {
		return rightHandExpression;
	}

	public ComparisonOperator getOperator() {
		return operator;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitRelationalPredicate( this );
	}
}
