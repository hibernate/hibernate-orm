/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.OracleJsonArrayBlobJdbcType;

/**
 * Specialized type mapping for {@code JSON} and the JSON SQL data type for Oracle.
 *
 * @author Christian Beikov
 */
public class OracleJsonArrayJdbcType extends OracleJsonArrayBlobJdbcType {
	/**
	 * Singleton access
	 */
	public static final OracleJsonArrayJdbcType INSTANCE = new OracleJsonArrayJdbcType();

	private OracleJsonArrayJdbcType() {
	}

	@Override
	public String toString() {
		return "OracleJsonJdbcType";
	}

	@Override
	public String getCheckCondition(String columnName, JavaType<?> javaType, BasicValueConverter<?, ?> converter, Dialect dialect) {
		// No check constraint necessary, because the JSON DDL type is already OSON encoded
		return null;
	}
}
