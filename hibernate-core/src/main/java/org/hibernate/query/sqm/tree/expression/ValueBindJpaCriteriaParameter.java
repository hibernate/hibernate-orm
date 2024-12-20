/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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

	public ValueBindJpaCriteriaParameter(
			BindableType<? super T> type,
			T value,
			NodeBuilder nodeBuilder) {
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
		if ( existing != null ) {
			return existing;
		}
		return context.registerCopy( this, new ValueBindJpaCriteriaParameter<>( this ) );
	}

	public T getValue() {
		return value;
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( value );
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
		if ( this == anotherParameter ) {
			return 0;
		}
		return 1;
	}
}
