/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import org.hibernate.type.descriptor.WrapperOptions;

import java.sql.Timestamp;

public class SpannerTimeJdbcType extends TimeJdbcType {

	public static final TimeJdbcType INSTANCE = new SpannerTimeJdbcType();

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return Timestamp.class;
	}
}
