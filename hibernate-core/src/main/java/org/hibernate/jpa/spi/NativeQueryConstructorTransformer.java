/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.spi;

import org.hibernate.InstantiationException;
import org.hibernate.query.TupleTransformer;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * A {@link TupleTransformer} for handling {@link List} results from native queries.
 *
 * @author Gavin King
 */
public class NativeQueryConstructorTransformer<T> implements TupleTransformer<T> {

	private final Class<T> resultClass;
	private Constructor<T> constructor;

	private Constructor<T> constructor(Object[] elements) {
		if ( constructor == null ) {
			try {
				// we cannot be sure of the "true" parameter types
				// of the constructor we're looking for, so we need
				// to do something a bit weird here: match on just
				// the number of parameters
				for ( final Constructor<?> candidate : resultClass.getDeclaredConstructors() ) {
					final Class<?>[] parameterTypes = candidate.getParameterTypes();
					if ( parameterTypes.length == elements.length ) {
						// found a candidate with the right number
						// of parameters
						if ( constructor == null ) {
							constructor = resultClass.getDeclaredConstructor( parameterTypes );
							constructor.setAccessible( true );
						}
						else {
							// ambiguous, more than one constructor
							// with the right number of parameters
							constructor = null;
							break;
						}
					}
				}
			}
			catch (Exception e) {
				throw new InstantiationException( "Cannot instantiate query result type", resultClass, e );
			}
			if ( constructor == null ) {
				throw new InstantiationException( "Result class must have a single constructor with exactly "
						+ elements.length + " parameters", resultClass );
			}
		}
		return constructor;
	}

	public NativeQueryConstructorTransformer(Class<T> resultClass) {
		this.resultClass = resultClass;
	}

	@Override
	public T transformTuple(Object[] tuple, String[] aliases) {
		try {
			return constructor( tuple ).newInstance( tuple );
		}
		catch (Exception e) {
			throw new InstantiationException( "Cannot instantiate query result type", resultClass, e );
		}
	}
}
