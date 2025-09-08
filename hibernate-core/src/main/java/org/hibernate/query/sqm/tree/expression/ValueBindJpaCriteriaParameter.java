/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.type.BindableType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;

import java.util.Objects;


/**
 * A {@link JpaCriteriaParameter} created from a value when
 * {@link org.hibernate.query.criteria.ValueHandlingMode} is {@code BIND}.
 *
 * @see org.hibernate.query.criteria.ValueHandlingMode
 */
public class ValueBindJpaCriteriaParameter<T> extends JpaCriteriaParameter<T> {
	private final T value;

	public ValueBindJpaCriteriaParameter(BindableType<? super T> type, T value, NodeBuilder nodeBuilder) {
		super( null, type, false, nodeBuilder );
		assert value == null || type == null
			|| ( type instanceof SqmBindableType<? super T> bindable
					// TODO: why does SqmExpressible.getJavaType() return an apparently-wrong type?
					? bindable.getExpressibleJavaType().isInstance( value )
					: type.getJavaType().isInstance( value ) );
		this.value = value;
	}

	private ValueBindJpaCriteriaParameter(ValueBindJpaCriteriaParameter<T> original) {
		super( original );
		this.value = original.value;
	}

	@Override
	public ValueBindJpaCriteriaParameter<T> copy(SqmCopyContext context) {
		final ValueBindJpaCriteriaParameter<T> existing = context.getCopy( this );
		return existing != null
				? existing
				: context.registerCopy( this, new ValueBindJpaCriteriaParameter<>( this ) );
	}

	public T getValue() {
		return value;
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		SqmLiteral.appendHqlString( hql, getJavaTypeDescriptor(), value );
	}

	@Override
	public int compareTo(SqmParameter<T> parameter) {
		return Integer.compare( hashCode(), parameter.hashCode() );
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj instanceof ValueBindJpaCriteriaParameter<?> that ) {
			if ( value == null ) {
				return that.value == null && Objects.equals( getNodeType(), that.getNodeType() );
			}
			final var javaType = getJavaTypeDescriptor();
			if ( that.value != null ) {
				if ( javaType != null ) {
					//noinspection unchecked
					return javaType.equals( that.getJavaTypeDescriptor() ) && javaType.areEqual( value, (T) that.value );
				}
				else {
					return that.getJavaTypeDescriptor() == null && value.equals( that.value );
				}
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		if ( value == null ) {
			return 0;
		}
		final var javaType = getJavaTypeDescriptor();
		return javaType == null ? value.hashCode() : javaType.extractHashCode( value );
	}
}
