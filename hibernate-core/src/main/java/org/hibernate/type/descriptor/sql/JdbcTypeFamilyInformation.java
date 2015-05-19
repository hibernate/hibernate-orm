/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql;

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

	public static enum Family {
		BINARY( Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY ),
		NUMERIC( Types.BIGINT, Types.DECIMAL, Types.DOUBLE, Types.FLOAT, Types.INTEGER, Types.NUMERIC, Types.REAL, Types.SMALLINT, Types.TINYINT ),
		CHARACTER( Types.CHAR, Types.LONGNVARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR, Types.VARCHAR ),
		DATETIME( Types.DATE, Types.TIME, Types.TIMESTAMP ),
		CLOB( Types.CLOB, Types.NCLOB );

		private final int[] typeCodes;

		private Family(int... typeCodes) {
			this.typeCodes = typeCodes;

			for ( final int typeCode : typeCodes ) {
				JdbcTypeFamilyInformation.INSTANCE.typeCodeToFamilyMap.put( typeCode, this );
			}
		}

		public int[] getTypeCodes() {
			return typeCodes;
		}
	}

	private ConcurrentHashMap<Integer,Family> typeCodeToFamilyMap = new ConcurrentHashMap<Integer, Family>();

	/**
	 * Will return {@code null} if no match is found.
	 *
	 * @param typeCode The JDBC type code.
	 *
	 * @return The family of datatypes the type code belongs to, or {@code null} if it belongs to no known families.
	 */
	public Family locateJdbcTypeFamilyByTypeCode(int typeCode) {
		return typeCodeToFamilyMap.get( Integer.valueOf( typeCode ) );
	}
}
