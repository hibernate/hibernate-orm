/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.transform;

import java.lang.reflect.Constructor;

import org.hibernate.InstantiationException;
import org.hibernate.query.TypedTupleTransformer;

/**
 * Wraps the tuples in a constructor call.
 *
 * @deprecated since {@link ResultTransformer} is deprecated
 */
@Deprecated
public class AliasToBeanConstructorResultTransformer<T> implements ResultTransformer<T>, TypedTupleTransformer<T> {

	private final Constructor<T> constructor;

	/**
	 * Instantiates a AliasToBeanConstructorResultTransformer.
	 *
	 * @param constructor The constructor in which to wrap the tuples.
	 */
	public AliasToBeanConstructorResultTransformer(Constructor<T> constructor) {
		this.constructor = constructor;
	}

	@Override
	public Class<T> getTransformedType() {
		return constructor.getDeclaringClass();
	}

	/**
	 * Wrap the incoming tuples in a call to our configured constructor.
	 */
	@Override
	public T transformTuple(Object[] tuple, String[] aliases) {
		try {
			return constructor.newInstance( tuple );
		}
		catch ( Exception e ) {
			throw new InstantiationException( "Could not instantiate class", constructor.getDeclaringClass(), e );
		}
	}

	/**
	 * Define our hashCode by our defined constructor's hasCode.
	 *
	 * @return Our defined ctor hashCode
	 */
	@Override
	public int hashCode() {
		return constructor.hashCode();
	}

	/**
	 * 2 AliasToBeanConstructorResultTransformer are considered equal if they have the same
	 * defined constructor.
	 *
	 * @param other The other instance to check for equality.
	 * @return True if both have the same defined constructor; false otherwise.
	 */
	@Override
	public boolean equals(Object other) {
		return other instanceof AliasToBeanConstructorResultTransformer<?> transformer
			&& constructor.equals( transformer.constructor );
	}
}
