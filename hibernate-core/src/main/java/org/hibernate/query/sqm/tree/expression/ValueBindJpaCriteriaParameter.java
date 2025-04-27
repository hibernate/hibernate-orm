/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.BindableType;
import org.hibernate.query.sqm.NodeBuilder;
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
		assert value == null || type == null || type.isInstance( value );
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
	// TODO: fix this
	public int compareTo(SqmParameter parameter) {
		return this == parameter ? 0 : 1;
	}

	// this is not really a parameter, it's really a literal value
	// so use value equality based on its value

	@Override
	public boolean equals(Object object) {
		return object instanceof ValueBindJpaCriteriaParameter<?> that
			&& Objects.equals( this.value, that.value );
//			&& getJavaTypeDescriptor().areEqual( this.value, (T) that.value );
	}

	@Override
	public int hashCode() {
		return value == null ? 0 : value.hashCode(); // getJavaTypeDescriptor().extractHashCode( value );
	}

//	@Override
//	public boolean equals(Object object) {
//		return this == object;
//	}
//
//	@Override
//	public int hashCode() {
//		return System.identityHashCode( this );
//	}
}
