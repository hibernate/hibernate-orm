/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.converter.internal;

import java.util.Arrays;

import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * @author Gavin King
 */
public class EnumHelper {

	public static String[] getEnumeratedValues(Type type) {
		return getEnumeratedValues( type.getReturnedClass(), ( (BasicType<?>) type ).getJdbcType() );
	}

	public static String[] getEnumeratedValues(Class<?> javaType, JdbcType jdbcType) {
		//noinspection unchecked
		final Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) javaType;
		final String[] enumValues;
		switch ( jdbcType.getDefaultSqlTypeCode() ) {
			case SqlTypes.ENUM:
			case SqlTypes.NAMED_ENUM:
				enumValues = getSortedEnumeratedValues( enumClass );
				break;
			default:
				enumValues = getEnumeratedValues( enumClass );
				break;
		}
		return enumValues;
	}

	public static String[] getEnumeratedValues(Class<? extends Enum<?>> enumClass) {
		Enum<?>[] values = enumClass.getEnumConstants();
		String[] names = new String[values.length];
		for ( int i = 0; i < values.length; i++ ) {
			names[i] = values[i].name();
		}
		return names;
	}

	public static String[] getSortedEnumeratedValues(Class<? extends Enum<?>> enumClass) {
		final String[] names = getEnumeratedValues( enumClass );
		Arrays.sort( names );
		return names;
	}
}
