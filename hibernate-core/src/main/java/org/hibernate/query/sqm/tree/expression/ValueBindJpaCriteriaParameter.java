/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.BindableType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.SqmCopyContext;

/**
 * It is a JpaCriteriaParameter created from a value when ValueHandlingMode is equal to BIND
 *
 * @see org.hibernate.query.criteria.ValueHandlingMode
 */
public class ValueBindJpaCriteriaParameter<T> extends JpaCriteriaParameter<T>{
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
	public void appendHqlString(StringBuilder hql) {
		hql.append( value );
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode( this );
	}

	@Override
	public int compareTo(SqmParameter anotherParameter) {
		return this == anotherParameter ? 0 : 1;
	}
}
