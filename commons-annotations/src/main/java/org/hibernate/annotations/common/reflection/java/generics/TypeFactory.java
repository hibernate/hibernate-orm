/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.annotations.common.reflection.java.generics;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * This class instances our own <code>ParameterizedTypes</code> and <code>GenericArrayTypes</code>.
 * These are not supposed to be mixed with Java's implementations - beware of
 * equality/identity problems.
 *
 * @author Paolo Perrotta
 */
class TypeFactory {

	static ParameterizedType createParameterizedType(
			final Type rawType, final Type[] substTypeArgs,
			final Type ownerType
	) {
		return new ParameterizedType() {

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
				return Arrays.equals( getActualTypeArguments(), other.getActualTypeArguments() )
						&& safeEquals( getRawType(), other.getRawType() ) && safeEquals(
						getOwnerType(), other.getOwnerType()
				);
			}

			@Override
			public int hashCode() {
				return safeHashCode( getActualTypeArguments() ) ^ safeHashCode( getRawType() ) ^ safeHashCode(
						getOwnerType()
				);
			}
		};
	}

	static Type createArrayType(Type componentType) {
		if ( componentType instanceof Class ) {
			return Array.newInstance( (Class) componentType, 0 ).getClass();
		}
		return TypeFactory.createGenericArrayType( componentType );
	}

	private static GenericArrayType createGenericArrayType(final Type componentType) {
		return new GenericArrayType() {

			public Type getGenericComponentType() {
				return componentType;
			}

			@Override
			public boolean equals(Object obj) {
				if ( !( obj instanceof GenericArrayType ) ) {
					return false;
				}
				GenericArrayType other = (GenericArrayType) obj;
				return safeEquals( getGenericComponentType(), other.getGenericComponentType() );
			}

			@Override
			public int hashCode() {
				return safeHashCode( getGenericComponentType() );
			}
		};
	}

	private static boolean safeEquals(Type t1, Type t2) {
		if ( t1 == null ) {
			return t2 == null;
		}
		return t1.equals( t2 );
	}

	private static int safeHashCode(Object o) {
		if ( o == null ) {
			return 1;
		}
		return o.hashCode();
	}
}