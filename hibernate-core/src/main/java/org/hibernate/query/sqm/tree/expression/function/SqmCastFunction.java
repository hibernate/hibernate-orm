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
	private final SqmCastTarget castTarget;

	public SqmCastFunction(
			SqmExpression expressionToCast,
			SqmCastTarget castTarget) {
		super( castTarget.getExpressableType() );
		this.expressionToCast = expressionToCast;
		this.castTarget = castTarget;
	}

	public SqmExpression getExpressionToCast() {
		return expressionToCast;
	}

	public SqmCastTarget getCastTarget() {
		return castTarget;
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
