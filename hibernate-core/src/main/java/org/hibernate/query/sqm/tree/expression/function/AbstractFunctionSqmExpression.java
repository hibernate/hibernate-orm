/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.persister.queryable.spi.BasicValuedExpressableType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractFunctionSqmExpression implements FunctionSqmExpression {
	private final BasicValuedExpressableType resultType;

	public AbstractFunctionSqmExpression(BasicValuedExpressableType resultType) {
		this.resultType = resultType;
	}

	@Override
	public BasicValuedExpressableType getFunctionResultType() {
		return resultType;
	}

	@Override
	public BasicValuedExpressableType getExpressionType() {
		return getFunctionResultType();
	}

	@Override
	public BasicValuedExpressableType getInferableType() {
		return getFunctionResultType();
	}
}
