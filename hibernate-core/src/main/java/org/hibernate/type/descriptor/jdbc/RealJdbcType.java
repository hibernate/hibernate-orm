/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.Types;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#REAL REAL} handling.
 *
 * @author Steve Ebersole
 *
 * @deprecated use {@link FloatJdbcType}
 */
@Deprecated
public class RealJdbcType extends FloatJdbcType {
	public static final RealJdbcType INSTANCE = new RealJdbcType();

	public RealJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.REAL;
	}

	@Override
	public String getFriendlyName() {
		return "REAL";
	}

	@Override
	public String toString() {
		return "RealTypeDescriptor";
	}

	@Override
	public JavaType<?> getRecommendedJavaType(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().resolveDescriptor( Float.class );
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return Float.class;
	}

}
