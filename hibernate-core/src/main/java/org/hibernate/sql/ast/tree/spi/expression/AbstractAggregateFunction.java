/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractAggregateFunction extends AbstractStandardFunction implements AggregateFunction {
	private final Expression argument;
	private final boolean distinct;
	private final BasicValuedExpressableType resultType;

	public AbstractAggregateFunction(Expression argument, boolean distinct, BasicValuedExpressableType resultType) {
		this.argument = argument;
		this.distinct = distinct;
		this.resultType = resultType;
	}

	@Override
	public boolean isDistinct() {
		return distinct;
	}

	@Override
	public BasicValuedExpressableType getType() {
		return resultType;
	}

	@Override
	public Expression getArgument() {
		return argument;
	}
}
