/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class SqmCastFunction extends AbstractSqmFunction implements SqmFunction {
	public static final String NAME = "cast";

	private final SqmExpression expressionToCast;
	private final String explicitSqlCastTarget;

	public SqmCastFunction(
			SqmExpression expressionToCast,
			AllowableFunctionReturnType castTargetType) {
		this( expressionToCast, castTargetType, null );
	}

	public SqmCastFunction(
			SqmExpression expressionToCast,
			AllowableFunctionReturnType castTargetType,
			String explicitSqlCastTarget) {
		super( castTargetType );
		this.expressionToCast = expressionToCast;
		this.explicitSqlCastTarget = explicitSqlCastTarget;
	}

	public SqmExpression getExpressionToCast() {
		return expressionToCast;
	}

	public String getExplicitSqlCastTarget() {
		return explicitSqlCastTarget;
	}

	@Override
	public String getFunctionName() {
		return NAME;
	}

	@Override
	public boolean hasArguments() {
		return true;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitCastFunction( this );
	}

	@Override
	public String asLoggableText() {
		return "CAST(" + expressionToCast.asLoggableText() + ")";
	}
}
