/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.collectionbasictype;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.usertype.StaticUserTypeSupport;

/**
 * @author Vlad Mihalcea
 */
public class CommaDelimitedStringsMapType extends StaticUserTypeSupport<Map<String,String>> {
	public CommaDelimitedStringsMapType() {
		super(
				new CommaDelimitedStringMapJavaType(),
				VarcharJdbcType.INSTANCE
		);
	}

	@Override
	public CommaDelimitedStringMapJavaType getJavaType() {
		return (CommaDelimitedStringMapJavaType) super.getJavaType();
	}

	@Override
	public Map<String,String> nullSafeGet(ResultSet rs, int position, WrapperOptions options)
			throws SQLException {
		final Object extracted = getJdbcValueExtractor().extract( rs, position, options );
		if ( extracted == null ) {
			return null;
		}

		return getJavaType().fromString( (String) extracted );
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Map<String,String> value, int index, WrapperOptions options)
			throws SQLException {
		final String stringValue = getJavaType().toString( value );
		getJdbcValueBinder().bind( st, stringValue, index, options );
	}
}
