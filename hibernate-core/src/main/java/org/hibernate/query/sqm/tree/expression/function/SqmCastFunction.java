/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;

/**
 * @author Steve Ebersole
 */
public class SqmCastFunction extends AbstractSqmFunction implements SqmFunction {
	public static final String NAME = "cast";

	private final SqmExpression expressionToCast;

	public SqmCastFunction(
			SqmExpression expressionToCast,
			BasicValuedExpressableType<?> castTargetType) {
		super( castTargetType );
		this.expressionToCast = expressionToCast;
	}

	@Override
	public BasicValuedExpressableType getExpressableType() {
		return (BasicValuedExpressableType) super.getExpressableType();
	}

	public SqmExpression getExpressionToCast() {
		return expressionToCast;
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
