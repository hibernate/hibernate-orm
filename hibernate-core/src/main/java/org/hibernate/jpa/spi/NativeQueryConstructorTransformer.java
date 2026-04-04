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
		final var signature = new StringBuilder();
		signature.append( ctor.getDeclaringClass().getSimpleName() ).append( "(" );
		final var params = ctor.getParameterTypes();
		for ( int i = 0; i < params.length; i++ ) {
			if ( i > 0 ) {
				signature.append( ", " );
			}
			signature.append( params[i].getSimpleName() );
		}
		signature.append( ")" );
		return signature.toString();
	}

	private Constructor<T> constructor(Object[] elements) {
		if ( constructor == null ) {
			try {
				// we cannot be sure of the "true" parameter types
				// of the constructor we're looking for, so we need
				// to do something a bit weird here: match on just
				// the number of parameters
				for ( final var candidate : resultClass.getDeclaredConstructors() ) {
					final var parameterTypes = candidate.getParameterTypes();
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
				final var message = new StringBuilder();
				message.append( "Result class" )
						.append( " must have exactly one constructor with " )
						.append( elements.length )
						.append( " parameters - found ['" );
				boolean first = true;
				for ( var c : resultClass.getDeclaredConstructors() ) {
					if ( c.getParameterCount() == elements.length ) {
						if ( !first ) {
							message.append( "', '" );
						}
						message.append( constructorSignature( c ) );
						first = false;
					}
				}

				message.append( "'] in" );

				throw new InstantiationException( message.toString(), resultClass );
			}
		}
		return constructor;
	}

	@Override
	public T transformTuple(Object[] tuple, String[] aliases) {
		final var ctor = constructor( tuple );
		try {
			return ctor.newInstance( tuple );
		}
		catch (Exception e) {
			final var message = new StringBuilder();
			message.append( "Could not instantiate query result type - expected '" )
					.append( constructorSignature( ctor ) )
					.append( "' but found '" )
					.append( ctor.getDeclaringClass().getSimpleName() )
					.append( '(' );

			for ( int i = 0; i < tuple.length; i++ ) {
				final Object value = tuple[i];
				if ( i > 0 ) {
					message.append( ", " );
				}
				message.append( value == null ? "null" : value.getClass().getSimpleName() );
			}
			message.append( ")' in" );
			throw new InstantiationException( message.toString(), resultClass, e );
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
