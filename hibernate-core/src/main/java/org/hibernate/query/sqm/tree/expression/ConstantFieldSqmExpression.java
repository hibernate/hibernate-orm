/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.lang.reflect.Field;

import org.hibernate.metamodel.queryable.spi.BasicValuedExpressableType;
import org.hibernate.metamodel.queryable.spi.ExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;

/**
 * Represents a constant that came from a static field reference.
 *
 * @author Steve Ebersole
 */
public class ConstantFieldSqmExpression<T> implements ConstantSqmExpression<T> {
	private final Field sourceField;
	private final T value;

	private BasicValuedExpressableType typeDescriptor;

	public ConstantFieldSqmExpression(Field sourceField, T value) {
		this( sourceField, value, null );
	}

	public ConstantFieldSqmExpression(Field sourceField, T value, BasicValuedExpressableType typeDescriptor) {
		this.sourceField = sourceField;
		this.value = value;
		this.typeDescriptor = typeDescriptor;
	}

	public Field getSourceField() {
		return sourceField;
	}

	@Override
	public T getValue() {
		return value;
	}

	@Override
	public BasicValuedExpressableType getExpressionType() {
		return typeDescriptor;
	}

	@Override
	public BasicValuedExpressableType getInferableType() {
		return getExpressionType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void impliedType(ExpressableType type) {
		if ( type != null ) {
			this.typeDescriptor = (BasicValuedExpressableType) type;
		}
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitConstantFieldExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "ConstantField(" + value + ")";
	}
}
