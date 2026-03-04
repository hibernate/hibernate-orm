/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;


import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.GetObjectExtractor;
import org.hibernate.type.descriptor.jdbc.internal.SetObjectBinder;

import java.sql.Timestamp;

public class SpannerLocalTimeJdbcType extends LocalTimeJdbcType {

	public static final LocalTimeJdbcType INSTANCE = new SpannerLocalTimeJdbcType();

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new SetObjectBinder<>( javaType, this, Timestamp.class, getDdlTypeCode() );
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new GetObjectExtractor<>( javaType, this, Timestamp.class );
	}
}
