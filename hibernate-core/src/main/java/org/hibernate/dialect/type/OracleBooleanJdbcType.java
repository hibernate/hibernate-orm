/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BooleanJdbcType;

public class OracleBooleanJdbcType extends BooleanJdbcType {

	public static final OracleBooleanJdbcType INSTANCE = new OracleBooleanJdbcType();

	@Override
	public int getDdlTypeCode() {
		return SqlTypes.BIT;
	}

	@Override
	public String getCheckCondition(String columnName, JavaType<?> javaType, BasicValueConverter<?, ?> converter, Dialect dialect) {
		return columnName + " in (0,1)";
	}
}
