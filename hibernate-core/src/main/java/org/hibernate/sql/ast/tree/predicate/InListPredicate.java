/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.predicate;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * @author Steve Ebersole
 */
public class InListPredicate extends AbstractPredicate {
	private final Expression testExpression;
	private final List<Expression> listExpressions;

	public InListPredicate(Expression testExpression) {
		this( testExpression, new ArrayList<>() );
	}

	public InListPredicate(Expression testExpression, boolean negated, JdbcMappingContainer expressionType) {
		this( testExpression, new ArrayList<>(), negated, expressionType );
	}

	public InListPredicate(Expression testExpression, Expression... listExpressions) {
		this( testExpression, ArrayHelper.toExpandableList( listExpressions ) );
	}

	public InListPredicate(
			Expression testExpression,
			List<Expression> listExpressions) {
		this( testExpression, listExpressions, false, null );
	}

	public InListPredicate(
			Expression testExpression,
			List<Expression> listExpressions,
			boolean negated,
			JdbcMappingContainer expressionType) {
		super( expressionType, negated );
		this.testExpression = testExpression;
		this.listExpressions = listExpressions;
	}

	public Expression getTestExpression() {
		return testExpression;
	}

	public List<Expression> getListExpressions() {
		return listExpressions;
	}

	public void addExpression(Expression expression) {
		listExpressions.add( expression );
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitInListPredicate( this );
	}
}
