/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmAggregateFunction
		extends AbstractSqmFunction
		implements SqmAggregateFunction {
	private final SqmExpression argument;

	private boolean distinct;

	protected AbstractSqmAggregateFunction(SqmExpression argument, AllowableFunctionReturnType resultType) {
		super( resultType );
		this.argument = argument;
	}

	@Override
	public void makeDistinct() {
		distinct = true;
	}

	@Override
	public BasicValuedExpressableType getExpressableType() {
		return (BasicValuedExpressableType) super.getExpressableType();
	}

	@Override
	public BasicValuedExpressableType getInferableType() {
		return getExpressableType();
	}

	@Override
	public boolean hasArguments() {
		return true;
	}

	@Override
	public SqmExpression getArgument() {
		return argument;
	}

	@Override
	public boolean isDistinct() {
		return distinct;
	}
}
