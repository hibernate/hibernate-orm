/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.internal;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

import org.hibernate.models.spi.ParameterizedTypeDetails;

import static org.hibernate.models.spi.TypeDetails.Kind.PARAMETERIZED_TYPE;

public class ParameterizedTypeImpl implements ParameterizedType {

	private final Type[] substTypeArgs;
	private final Type rawType;
	private final Type ownerType;

	public ParameterizedTypeImpl(Type rawType, Type[] substTypeArgs, Type ownerType) {
		this.substTypeArgs = substTypeArgs;
		this.rawType = rawType;
		this.ownerType = ownerType;
	}

	public static ParameterizedTypeImpl from(ParameterizedTypeDetails typeDetails) {
		final var attributeType = typeDetails.determineRawClass().toJavaClass();
		final var arguments = typeDetails.asParameterizedType().getArguments();
		final int argumentsSize = arguments.size();
		final var argumentTypes = new Type[argumentsSize];
		for ( int i = 0; i < argumentsSize; i++ ) {
			final var argument = arguments.get( i );
			argumentTypes[i] =
					argument.getTypeKind() == PARAMETERIZED_TYPE
							? from( argument.asParameterizedType() )
							: argument.determineRawClass().toJavaClass();
		}
		final var owner = typeDetails.asParameterizedType().getOwner();
		final var ownerType = owner == null ? null : owner.determineRawClass().toJavaClass();
		return new ParameterizedTypeImpl( attributeType, argumentTypes, ownerType );
	}

	@Override
	public Type[] getActualTypeArguments() {
		return substTypeArgs;
	}

	@Override
	public Type getRawType() {
		return rawType;
	}

	@Override
	public Type getOwnerType() {
		return ownerType;
	}

	@Override
	public boolean equals(Object obj) {
		if ( !(obj instanceof ParameterizedType other) ) {
			return false;
		}
		return Objects.equals( getOwnerType(), other.getOwnerType() )
			&& Objects.equals( getRawType(), other.getRawType() )
			&& Arrays.equals( getActualTypeArguments(), other.getActualTypeArguments() );
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode( getActualTypeArguments() )
				^ Objects.hashCode( getOwnerType() )
				^ Objects.hashCode( getRawType() );
	}

	@Override
	public String toString() {
		final var typeExpression = new StringBuilder();
		if ( ownerType != null ) {
			typeExpression.append( ownerType.getTypeName() ).append( "$" );
			if ( ownerType instanceof ParameterizedType parameterizedType ) {
				// Find the simple name of the nested type by
				// removing the shared prefix with the outer type.
				final int prefixLength =
						parameterizedType.getRawType().getTypeName().length()
						+ 1; // account for the '$' separator
				typeExpression.append( rawType.getTypeName().substring( prefixLength ) );
			}
			else if ( rawType instanceof Class<?> clazz ) {
				typeExpression.append( clazz.getSimpleName() );
			}
			else {
				typeExpression.append( rawType.getTypeName() );
			}
		}
		else {
			typeExpression.append( rawType.getTypeName() );
		}

		if ( substTypeArgs != null ) {
			final var argList = new StringJoiner( ", ", "<", ">" );
			argList.setEmptyValue( "" );
			for ( var type : substTypeArgs ) {
				argList.add( type.getTypeName() );
			}
			typeExpression.append( argList );
		}

		return typeExpression.toString();
	}
}
