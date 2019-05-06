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
public class SqmCastFunction<T> extends AbstractSqmFunction<T> {
	public static final String NAME = "cast";

	private final SqmExpression<?> expressionToCast;
	private final SqmCastTarget<T> castTarget;

	public SqmCastFunction(
			SqmExpression<?> expressionToCast,
			SqmCastTarget<T> castTarget) {
		super( castTarget.getType(), expressionToCast.nodeBuilder() );
		this.expressionToCast = expressionToCast;
		this.castTarget = castTarget;
	}

	public SqmCastFunction(
			SqmExpression<?> expressionToCast,
			AllowableFunctionReturnType<T> resultType) {
		this(
				expressionToCast,
				new SqmCastTarget<>( resultType, expressionToCast.nodeBuilder() )
		);
	}

	public SqmExpression<?> getExpressionToCast() {
		return expressionToCast;
	}

	public SqmCastTarget<T> getCastTarget() {
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
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCastFunction( this );
	}

	@Override
	public String asLoggableText() {
		return "CAST(" + expressionToCast.asLoggableText() + ")";
	}
}
