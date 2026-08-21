/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.Types;

/**
 * Descriptor for {@link Types#LONGVARBINARY LONGVARBINARY} handling.
 *
 * @author Steve Ebersole
 */
public class LongVarbinaryJdbcType extends VarbinaryJdbcType {
	public static final LongVarbinaryJdbcType INSTANCE = new LongVarbinaryJdbcType();

	private final int defaultSqlTypeCode;
	private final int ddlTypeCode;

	public LongVarbinaryJdbcType() {
		this( Types.LONGVARBINARY, Types.LONGVARBINARY );
	}

	public LongVarbinaryJdbcType(int defaultSqlTypeCode) {
		this( defaultSqlTypeCode, defaultSqlTypeCode );
	}

	public LongVarbinaryJdbcType(int defaultSqlTypeCode, int ddlTypeCode) {
		this.defaultSqlTypeCode = defaultSqlTypeCode;
		this.ddlTypeCode = ddlTypeCode;
	}

	@Override
	public String toString() {
		return "LongVarbinaryTypeDescriptor";
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.LONGVARBINARY;
	}

	@Override
	public int getDdlTypeCode() {
		return ddlTypeCode;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return defaultSqlTypeCode;
	}
}
