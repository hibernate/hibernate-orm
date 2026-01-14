/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.sql.SQLException;
import java.sql.Types;

public class SpannerArrayJdbcType extends ArrayJdbcType {

	public SpannerArrayJdbcType(JdbcType elementJdbcType) {
		super( elementJdbcType );
	}

	@Override
	protected <T, E> Object[] convertToArray(
			BasicBinder<T> binder,
			ValueBinder<E> elementBinder,
			BasicPluralJavaType<E> pluralJavaType,
			T value,
			WrapperOptions options) throws SQLException {
		Object[] originalArray = super.convertToArray( binder, elementBinder, pluralJavaType, value, options );
		if ( originalArray != null && requiresWideningToLong( getElementJdbcType() ) ) {
			return widenToLongArray( originalArray );
		}
		return originalArray;
	}

	private boolean requiresWideningToLong(JdbcType elementJdbcType) {
		// Spanner only supports INT64, so it treats TINYINT, SMALLINT, and INTEGER as INT64.
		// The Spanner JDBC driver is strict and requires that array parameters bound to
		// INT64 columns be of type Long[]. Passing Integer[] (or Short[]/Byte[]) causes
		// an ArrayStoreException inside the driver during parameter binding.
		int code = elementJdbcType.getJdbcTypeCode();
		return code == Types.INTEGER || code == Types.SMALLINT || code == Types.TINYINT;
	}

	private Object[] widenToLongArray(Object[] originalArray) {
		Long[] longArray = new Long[originalArray.length];
		for ( int i = 0; i < originalArray.length; i++ ) {
			Number number = (Number) originalArray[i];
			longArray[i] = number == null ? null : number.longValue();
		}
		return longArray;
	}
}
