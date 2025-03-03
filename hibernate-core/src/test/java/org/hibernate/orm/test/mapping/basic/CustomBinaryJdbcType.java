/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.sql.Types;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType;

/**
 * JdbcType for documentation
 *
 * @author Steve Ebersole
 */
public class CustomBinaryJdbcType implements JdbcType {
	@Override
	public int getJdbcTypeCode() {
		return Types.VARBINARY;
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return VarbinaryJdbcType.INSTANCE.getBinder( javaType );
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return VarbinaryJdbcType.INSTANCE.getExtractor( javaType );
	}
}
