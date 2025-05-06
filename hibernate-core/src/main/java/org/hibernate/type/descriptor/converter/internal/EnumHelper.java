/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.converter.internal;

import java.util.Arrays;

import org.hibernate.HibernateException;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
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
		final Enum<?>[] values = enumClass.getEnumConstants();
		final String[] names = new String[values.length];
		for ( int i = 0; i < values.length; i++ ) {
			names[i] = values[i].name();
		}
		return names;
	}

	public static String[] getEnumeratedValues(
			Class<? extends Enum<?>> enumClass, BasicValueConverter<Enum<?>,?> converter) {
		final Enum<?>[] values = enumClass.getEnumConstants();
		final String[] names = new String[values.length];
		for ( int i = 0; i < values.length; i++ ) {
			final Object relationalValue = converter.toRelationalValue( values[i] );
			if ( relationalValue == null ) {
				throw new HibernateException( "Enum value converter returned null for enum class '" + enumClass.getName() + "'" );
			}
			names[i] = relationalValue.toString();
		}
		return names;
	}

	public static String[] getSortedEnumeratedValues(Class<? extends Enum<?>> enumClass) {
		final String[] names = getEnumeratedValues( enumClass );
		Arrays.sort( names );
		return names;
	}
}
