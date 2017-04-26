/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.persister.queryable.spi.ExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;

/**
 * @author Steve Ebersole
 */
public class ConstantEnumSqmExpression<T extends Enum> implements ConstantSqmExpression<T> {
	private final T value;
	private ExpressableType domainType;

	public ConstantEnumSqmExpression(T value) {
		this( value, null );
	}

	public ConstantEnumSqmExpression(T value, ExpressableType domainType) {
		this.value = value;
		this.domainType = domainType;
	}

	@Override
	public T getValue() {
		return value;
	}

	@Override
	public ExpressableType getExpressionType() {
		return domainType;
	}

	@Override
	public ExpressableType getInferableType() {
		return getExpressionType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void impliedType(ExpressableType expressableType) {
		this.domainType = domainType;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitConstantEnumExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "EnumConstant(" + value + ")";
	}
}
