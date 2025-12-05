/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.Types;
import java.util.Locale;

import org.hibernate.type.descriptor.JdbcTypeNameMapper;

/**
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
public final class LobTypeMappings {

	private LobTypeMappings() {
	}

	public static boolean isMappedToKnownLobCode(final int jdbcTypeCode) {
		return
				// BLOB mappings
				jdbcTypeCode == Types.BLOB ||
				jdbcTypeCode == Types.BINARY ||
				jdbcTypeCode == Types.VARBINARY ||
				jdbcTypeCode == Types.LONGVARBINARY ||

				// CLOB mappings
				jdbcTypeCode == Types.CLOB ||
				jdbcTypeCode == Types.CHAR ||
				jdbcTypeCode == Types.VARCHAR ||
				jdbcTypeCode == Types.LONGVARCHAR ||

				// NCLOB mappings
				jdbcTypeCode == Types.NCLOB ||
				jdbcTypeCode == Types.NCHAR ||
				jdbcTypeCode == Types.NVARCHAR ||
				jdbcTypeCode == Types.LONGNVARCHAR;
	}

	public static int getLobCodeTypeMapping(final int jdbcTypeCode) {
		return switch ( jdbcTypeCode ) {
			// BLOB mappings
			case Types.BLOB, Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> Types.BLOB;
			// CLOB mappings
			case Types.CLOB, Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR -> Types.CLOB;
			// NCLOB mappings
			case Types.NCLOB, Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR -> Types.NCLOB;
			// Anything else:
			default -> throw new IllegalArgumentException(
					String.format(
							Locale.ROOT,
							"JDBC type-code [%s (%s)] not known to have a corresponding LOB equivalent",
							jdbcTypeCode,
							JdbcTypeNameMapper.getTypeName( jdbcTypeCode )
					) );
		};
	}
}
