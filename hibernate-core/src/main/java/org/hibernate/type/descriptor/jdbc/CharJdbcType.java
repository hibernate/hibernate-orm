/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.Types;


/**
 * Descriptor for {@link Types#CHAR CHAR} handling.
 *
 * @author Steve Ebersole
 */
public class CharJdbcType extends VarcharJdbcType {
	public static final CharJdbcType INSTANCE = new CharJdbcType();

	public CharJdbcType() {
	}

	@Override
	public String toString() {
		return "CharTypeDescriptor";
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.CHAR;
	}

	@Override
	protected int resolveIndicatedJdbcTypeCode(JdbcTypeIndicators indicators) {
		if ( indicators.isLob() ) {
			return indicators.isNationalized() ? Types.NCLOB : Types.CLOB;
		}
		else {
			return indicators.isNationalized() ? Types.NCHAR : Types.CHAR;
		}
	}
}
