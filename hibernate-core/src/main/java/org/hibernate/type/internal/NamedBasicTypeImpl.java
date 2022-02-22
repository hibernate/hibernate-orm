/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * @author Christian Beikov
 */
public class NamedBasicTypeImpl<J> extends BasicTypeImpl<J> {

	private final String name;

	public NamedBasicTypeImpl(JavaType<J> jtd, JdbcType std, String name) {
		super( jtd, std );
		this.name = name;
	}

	public NamedBasicTypeImpl(
			JavaType<J> javaType,
			JdbcType jdbcType,
			String sqlType,
			Integer lengthOrPrecision,
			Integer scale,
			String name) {
		super( javaType, jdbcType, sqlType, lengthOrPrecision, scale );
		this.name = name;
	}

	@Override
	public BasicType<J> withSqlType(String sqlType, Integer lengthOrPrecision, Integer scale) {
		return lengthOrPrecision == null && scale == null ? this : new NamedBasicTypeImpl<>(
				getJavaTypeDescriptor(),
				getJdbcType(),
				sqlType,
				lengthOrPrecision,
				scale,
				name
		);
	}

	@Override
	public String getName() {
		return name;
	}

}
