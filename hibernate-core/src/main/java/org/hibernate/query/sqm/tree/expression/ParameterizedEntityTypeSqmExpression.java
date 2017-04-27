/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.persister.queryable.spi.ExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;

/**
 * @author Steve Ebersole
 */
public class ParameterizedEntityTypeSqmExpression implements SqmExpression {
	private final ParameterSqmExpression parameterExpression;

	public ParameterizedEntityTypeSqmExpression(ParameterSqmExpression parameterExpression) {
		this.parameterExpression = parameterExpression;
	}

	@Override
	public ExpressableType getExpressionType() {
		return parameterExpression.getExpressionType();
	}

	@Override
	public ExpressableType getInferableType() {
		return parameterExpression.getExpressionType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitParameterizedEntityTypeExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "TYPE(" + parameterExpression.asLoggableText() + ")";
	}
}
