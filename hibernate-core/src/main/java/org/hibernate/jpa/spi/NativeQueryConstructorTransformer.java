/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.spi;

import java.lang.reflect.Constructor;

import org.hibernate.InstantiationException;
import org.hibernate.query.TupleTransformer;

/**
 * A {@link TupleTransformer} which packages each native query result in
 * an instance of the result class by calling an appropriate constructor.
 *
 * @implNote The result type must have exactly one constructor with the
 * correct number of parameters. Constructors cannot be disambiguated by
 * parameter type.
 *
 * @since 6.3
 *
 * @author Gavin King
 */
public class NativeQueryConstructorTransformer<T> implements TupleTransformer<T> {

	private final Class<T> resultClass;
	private transient Constructor<T> constructor;

	public NativeQueryConstructorTransformer(Class<T> resultClass) {
		this.resultClass = resultClass;
	}

	private static String constructorSignature(Constructor<?> ctor) {
		StringBuilder sb = new StringBuilder();
		sb.append( ctor.getDeclaringClass().getSimpleName() ).append( "(" );
		Class<?>[] params = ctor.getParameterTypes();
		for ( int i = 0; i < params.length; i++ ) {
			if ( i > 0 ) {
				sb.append( ", " );
			}
			sb.append( params[i].getSimpleName() );
		}
		sb.append( ")" );
		return sb.toString();
	}

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
				StringBuilder sb = new StringBuilder();
				sb.append( "Result class" )
						.append( " must have exactly one constructor with " )
						.append( elements.length )
						.append( " parameters, found: [" );
				boolean first = true;
				for ( Constructor<?> c : resultClass.getDeclaredConstructors() ) {
					if ( c.getParameterCount() == elements.length ) {
						if ( !first ) {
							sb.append( ", " );
						}
						sb.append( constructorSignature( c ) );
						first = false;
					}
				}

				sb.append( "]" );

				throw new InstantiationException( sb.toString(), resultClass );
			}
		}
		return constructor;
	}

	@Override
	public T transformTuple(Object[] tuple, String[] aliases) {
		Constructor<T> ctor = constructor( tuple );
		try {
			return ctor.newInstance( tuple );
		}
		catch (Exception e) {
			final StringBuilder sb = new StringBuilder();
			sb.append( "Cannot instantiate query result type, expected: " ).append( constructorSignature( ctor ) )
					.append( " but found (" );

			for ( int i = 0; i < tuple.length; i++ ) {
				final Object value = tuple[i];
				if ( i > 0 ) {
					sb.append( ", " );
				}
				sb.append( value == null ? "null" : value.getClass().getSimpleName() );
			}
			sb.append( ")" );
			throw new InstantiationException( sb.toString(), resultClass, e );
		}
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof NativeQueryConstructorTransformer<?> that
			&& this.resultClass == that.resultClass;
			// should be safe to ignore the cached constructor here
	}

	@Override
	public int hashCode() {
		return resultClass.hashCode();
	}
}
