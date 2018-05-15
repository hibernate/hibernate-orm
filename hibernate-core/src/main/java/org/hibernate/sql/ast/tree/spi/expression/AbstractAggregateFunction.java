/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractAggregateFunction extends AbstractStandardFunction implements AggregateFunction {
	private final Expression argument;
	private final boolean distinct;
	private final SqlExpressableType resultType;

	public AbstractAggregateFunction(Expression argument, boolean distinct, SqlExpressableType resultType) {
		this.argument = argument;
		this.distinct = distinct;
		this.resultType = resultType;
	}

	@Override
	public boolean isDistinct() {
		return distinct;
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return resultType;
	}

	@Override
	public SqlExpressableType getType() {
		return getExpressableType();
	}

	@Override
	public Expression getArgument() {
		return argument;
	}

	@Override
	public SqlExpressable getExpressable() {
		return this;
	}
}
