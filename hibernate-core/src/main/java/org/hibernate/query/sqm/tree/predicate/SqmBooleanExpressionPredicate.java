/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import java.util.Collections;
import java.util.List;
import javax.persistence.criteria.Expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.type.spi.BasicType;

/**
 * Represents an expression whose type is boolean, and can therefore be used as a predicate.
 *
 * @author Steve Ebersole
 */
public class SqmBooleanExpressionPredicate extends AbstractNegatableSqmPredicate {
	private final SqmExpression<Boolean> booleanExpression;

	public SqmBooleanExpressionPredicate(SqmExpression<Boolean> booleanExpression, NodeBuilder nodeBuilder) {
		super( nodeBuilder );

		assert booleanExpression.getNodeType() != null;
		final Class expressionJavaType = ( (BasicType) booleanExpression.getNodeType() ).getJavaType();
		assert boolean.class.equals( expressionJavaType ) || Boolean.class.equals( expressionJavaType );

		this.booleanExpression = booleanExpression;
	}

	public SqmExpression getBooleanExpression() {
		return booleanExpression;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitBooleanExpressionPredicate( this );
	}

	@Override
	public List<Expression<Boolean>> getExpressions() {
		return Collections.singletonList( booleanExpression );
	}
}
