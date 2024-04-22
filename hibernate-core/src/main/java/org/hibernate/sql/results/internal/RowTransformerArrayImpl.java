/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.lang.reflect.Array;

import org.hibernate.sql.results.spi.RowTransformer;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * RowTransformer used when an array is explicitly specified as the return type
 *
 * @author Steve Ebersole
 */
public class RowTransformerArrayImpl implements RowTransformer<Object[]> {
	/**
	 * Singleton access
	 */
	public static final RowTransformerArrayImpl INSTANCE = new RowTransformerArrayImpl();

	public static RowTransformerArrayImpl instance() {
		return INSTANCE;
	}

	@Override
	public Object[] transformRow(Object[] row) {
		return row;
	}

	@Override
	public Object[] transformRow(Object[] row, JavaType<?> resultJavaType) {
		final int size = row.length;
		final Object[] resultRowArray = (Object[]) getResultRowArray( resultJavaType.getJavaTypeClass(), size );
		for ( int i = 0; i < size; i++ ) {
			resultRowArray[i] = row[i];
		}
		return resultRowArray;
	}

	private Object getResultRowArray(Class<?> clazz, int size) {
		if ( clazz == Object.class ) {
			return new Object[size];
		}
		else if ( clazz == Integer.class ) {
			return new Integer[size];
		}
		else if ( clazz == Long.class ) {
			return new Long[size];
		}
		else if ( clazz == Double.class ) {
			return new Double[size];
		}
		else if ( clazz == Float.class ) {
			return new Float[size];
		}
		else if ( clazz == Short.class ) {
			return new Short[size];
		}
		else if ( clazz == Byte.class ) {
			return new Byte[size];
		}
		else if ( clazz == Boolean.class ) {
			return new Boolean[size];
		}
		else if ( clazz == Character.class ) {
			return new Character[size];
		}
		else if ( clazz == String.class ) {
			return new String[size];
		}
		return Array.newInstance( clazz, size );
	}
}
