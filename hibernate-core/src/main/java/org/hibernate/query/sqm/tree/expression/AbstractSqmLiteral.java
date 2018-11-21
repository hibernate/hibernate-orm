/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.function.Supplier;

import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmLiteral<T> extends AbstractInferableTypeSqmExpression implements SqmLiteral<T> {
	private T value;

	public AbstractSqmLiteral(T value, BasicValuedExpressableType inherentType) {
		super( inherentType );
		this.value = value;
	}

	@Override
	public T getLiteralValue() {
		return value;
	}

	@Override
	public BasicValuedExpressableType getExpressableType() {
		return (BasicValuedExpressableType) super.getExpressableType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Supplier<? extends BasicValuedExpressableType> getInferableType() {
		return (Supplier<? extends BasicValuedExpressableType>) super.getInferableType();
	}

	@Override
	public String asLoggableText() {
		return "Literal( " + value + ")";
	}
}
