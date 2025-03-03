/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.Types;

import org.hibernate.type.SqlTypes;

/**
 * Descriptor for {@link java.time.Duration}.
 *
 * @author Marco Belladelli
 */
public class DurationJdbcType extends NumericJdbcType {
	public static final DurationJdbcType INSTANCE = new DurationJdbcType();

	@Override
	public int getDdlTypeCode() {
		return Types.NUMERIC;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.DURATION;
	}

	@Override
	public String getFriendlyName() {
		return "DURATION";
	}

	@Override
	public String toString() {
		return "DurationJdbcType";
	}
}
