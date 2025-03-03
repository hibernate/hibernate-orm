/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.Types;

/**
 * Descriptor for {@link Types#NUMERIC NUMERIC} handling.
 *
 * @author Steve Ebersole
 */
public class NumericJdbcType extends DecimalJdbcType {
	public static final NumericJdbcType INSTANCE = new NumericJdbcType();

	public NumericJdbcType() {
	}

	@Override
	public String toString() {
		return "NumericTypeDescriptor";
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.NUMERIC;
	}
}
