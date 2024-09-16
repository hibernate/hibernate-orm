/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BooleanJdbcType;

public class OracleBooleanJdbcType extends BooleanJdbcType {

	public static final OracleBooleanJdbcType INSTANCE = new OracleBooleanJdbcType();

	@Override
	public String getCheckCondition(String columnName, JavaType<?> javaType, BasicValueConverter<?, ?> converter, Dialect dialect) {
		return columnName + " in (0,1)";
	}
}
