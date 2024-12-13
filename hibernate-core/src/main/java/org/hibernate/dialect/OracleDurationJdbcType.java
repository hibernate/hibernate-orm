/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

import java.sql.Types;

public class OracleDurationJdbcType extends VarcharJdbcType {

	public static final OracleDurationJdbcType INSTANCE = new OracleDurationJdbcType();

	public OracleDurationJdbcType() {
	}

	@Override
	public int getDdlTypeCode() {
		return Types.VARCHAR;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.DURATION;
	}
}
