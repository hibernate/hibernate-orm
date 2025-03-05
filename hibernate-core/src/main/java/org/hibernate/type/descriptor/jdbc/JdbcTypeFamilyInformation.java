/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;


import java.sql.Types;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Information pertaining to JDBC type families.
 *
 * @author Steve Ebersole
 */
public class JdbcTypeFamilyInformation {
	public static final JdbcTypeFamilyInformation INSTANCE = new JdbcTypeFamilyInformation();

	// todo : make Family non-enum so it can be expanded by Dialects?

	public enum Family {
		BINARY( Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY/*, SqlTypes.LONG32VARBINARY*/ ),
		NUMERIC( Types.BIGINT, Types.DECIMAL, Types.DOUBLE, Types.FLOAT, Types.INTEGER, Types.NUMERIC, Types.REAL, Types.SMALLINT, Types.TINYINT ),
		CHARACTER( Types.CHAR, Types.LONGNVARCHAR, Types.LONGVARCHAR, /*SqlTypes.LONG32NVARCHAR ,SqlTypes.LONG32VARCHAR,*/ Types.NCHAR, Types.NVARCHAR, Types.VARCHAR ),
		DATETIME( Types.DATE, Types.TIME, Types.TIME_WITH_TIMEZONE, Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE ),
		CLOB( Types.CLOB, Types.NCLOB );

		private final int[] typeCodes;

		Family(int... typeCodes) {
			this.typeCodes = typeCodes;
		}

		public int[] getTypeCodes() {
			return typeCodes;
		}
	}

	private final ConcurrentHashMap<Integer,Family> typeCodeToFamilyMap = new ConcurrentHashMap<>();
	{
		for ( Family family : Family.values() ) {
			for ( int typeCode : family.getTypeCodes() ) {
				typeCodeToFamilyMap.put( typeCode, family );
			}
		}
	}

	/**
	 * Will return {@code null} if no match is found.
	 *
	 * @param typeCode The JDBC type code.
	 *
	 * @return The family of datatypes the type code belongs to, or {@code null} if it belongs to no known families.
	 */
	public Family locateJdbcTypeFamilyByTypeCode(int typeCode) {
		return typeCodeToFamilyMap.get(typeCode);
	}
}
