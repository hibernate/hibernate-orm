/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.expression;

import org.hibernate.type.BasicType;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractAggregateFunction extends SelfReadingExpressionSupport implements AggregateFunction {
	private final Expression argument;
	private final boolean distinct;
	private final BasicType resultType;

	public AbstractAggregateFunction(Expression argument, boolean distinct, BasicType resultType) {
		this.argument = argument;
		this.distinct = distinct;
		this.resultType = resultType;
	}

	@Override
	public Expression getArgument() {
		return argument;
	}

	@Override
	public boolean isDistinct() {
		return distinct;
	}

	@Override
	public Type getType() {
		return resultType;
	}
}
