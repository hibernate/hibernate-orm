/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.usertype.inet;

import org.hibernate.dialect.type.PostgreSQLInetJdbcType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Vlad Mihalcea
 */
public class InetJdbcType extends PostgreSQLInetJdbcType {

	public static final InetJdbcType INSTANCE = new InetJdbcType();

	@Override
	public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer precision,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().getDescriptor( Inet.class );
	}

	@Override
	protected <X> X fromString(String string, JavaType<X> javaType, WrapperOptions options) {
		return javaType.wrap( string, options );
	}
}
