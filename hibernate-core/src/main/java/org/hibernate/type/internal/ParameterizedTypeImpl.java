/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.internal;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

public class ParameterizedTypeImpl implements ParameterizedType {

	private final Type[] substTypeArgs;
	private final Type rawType;
	private final Type ownerType;

	public ParameterizedTypeImpl(Type rawType, Type[] substTypeArgs, Type ownerType) {
		this.substTypeArgs = substTypeArgs;
		this.rawType = rawType;
		this.ownerType = ownerType;
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
		if ( !( obj instanceof ParameterizedType ) ) {
			return false;
		}
		ParameterizedType other = (ParameterizedType) obj;
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

			if ( ownerType instanceof ParameterizedType ) {
				// Find simple name of nested type by removing the
				// shared prefix with owner.
				sb.append(
						rawType.getTypeName().replace(
								( (ParameterizedType) ownerType ).getRawType().getTypeName() + "$",
								""
						)
				);
			}
			else if ( rawType instanceof Class<?> ) {
				sb.append( ( (Class<?>) rawType ).getSimpleName() );
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
