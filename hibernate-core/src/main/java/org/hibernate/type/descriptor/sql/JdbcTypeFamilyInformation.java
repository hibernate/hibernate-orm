/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
