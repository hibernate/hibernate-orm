/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.internal;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import jakarta.annotation.Nullable;
import org.hibernate.models.spi.ParameterizedTypeDetails;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.models.spi.TypeVariableScope;

public class ParameterizedTypeImpl implements ParameterizedType {

	private final Type[] substTypeArgs;
	private final Type rawType;
	private final Type ownerType;

	public ParameterizedTypeImpl(Type rawType, Type[] substTypeArgs, @Nullable Type ownerType) {
		this.substTypeArgs = substTypeArgs;
		this.rawType = rawType;
		this.ownerType = ownerType;
	}

	public static ParameterizedTypeImpl from(ParameterizedTypeDetails typeDetails) {
		final java.lang.reflect.Type attributeType = typeDetails.determineRawClass().toJavaClass();

		final List<TypeDetails> arguments = typeDetails.asParameterizedType().getArguments();
		final int argumentsSize = arguments.size();
		final java.lang.reflect.Type[] argumentTypes = new java.lang.reflect.Type[argumentsSize];
		for ( int i = 0; i < argumentsSize; i++ ) {
			argumentTypes[i] = arguments.get( i ).determineRawClass().toJavaClass();
		}
		final TypeVariableScope owner = typeDetails.asParameterizedType().getOwner();
		final java.lang.reflect.Type ownerType;
		if ( owner != null ) {
			ownerType = owner.determineRawClass().toJavaClass();
		}
		else {
			ownerType = null;
		}
		return new ParameterizedTypeImpl( attributeType, argumentTypes, ownerType );
	}

	public Type[] getActualTypeArguments() {
		return substTypeArgs;
	}

	public Type getRawType() {
		return rawType;
	}

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
		final StringBuilder sb = new StringBuilder();
		if ( ownerType != null ) {
			sb.append( ownerType.getTypeName() );

			sb.append( "$" );

			if ( ownerType instanceof ParameterizedType parameterizedType ) {
				// Find simple name of nested type by removing the
				// shared prefix with owner.
				sb.append(
						rawType.getTypeName().replace(
								parameterizedType.getRawType().getTypeName() + "$",
								""
						)
				);
			}
			else if ( rawType instanceof Class<?> clazz ) {
				sb.append( clazz.getSimpleName() );
			}
			else {
				sb.append( rawType.getTypeName() );
			}
		}
		else {
			sb.append( rawType.getTypeName() );
		}

		if ( substTypeArgs != null ) {
			final StringJoiner sj = new StringJoiner( ", ", "<", ">" );
			sj.setEmptyValue( "" );
			for ( Type t : substTypeArgs ) {
				sj.add( t.getTypeName() );
			}
			sb.append( sj );
		}

		return sb.toString();
	}
}
