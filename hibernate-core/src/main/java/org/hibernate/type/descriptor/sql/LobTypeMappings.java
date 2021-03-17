/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql;

import java.sql.Types;
import java.util.Locale;
import java.util.Map;

import org.hibernate.type.descriptor.JdbcTypeNameMapper;

/**
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
public final class LobTypeMappings {

	/**
	 * Singleton access
	 * @deprecated use the static method helpers instead.
	 */
	@Deprecated
	public static final LobTypeMappings INSTANCE = new LobTypeMappings();

	private LobTypeMappings() {
	}

	/**
	 *
	 * @param jdbcTypeCode
	 * @return true if corresponding Lob code exists; false otherwise.
	 * @deprecated use {@link #isMappedToKnownLobCode(int)}
	 */
	@Deprecated
	public boolean hasCorrespondingLobCode(final int jdbcTypeCode) {
		return isMappedToKnownLobCode( jdbcTypeCode );
	}

	/**
	 *
	 * @param jdbcTypeCode
	 * @return corresponding Lob code
	 * @deprecated use {@link #getLobCodeTypeMapping(int)}
	 */
	@Deprecated
	public int getCorrespondingLobCode(final int jdbcTypeCode) {
		return getLobCodeTypeMapping( jdbcTypeCode );
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
		switch ( jdbcTypeCode ) {

			// BLOB mappings
			case Types.BLOB :
			case Types.BINARY :
			case Types.VARBINARY :
			case Types.LONGVARBINARY : return Types.BLOB;

			// CLOB mappings
			case Types.CLOB :
			case Types.CHAR :
			case Types.VARCHAR :
			case Types.LONGVARCHAR : return Types.CLOB;

			// NCLOB mappings
			case Types.NCLOB :
			case Types.NCHAR :
			case Types.NVARCHAR :
			case Types.LONGNVARCHAR : return Types.NCLOB;

			// Anything else:
			default:
				throw new IllegalArgumentException(
						String.format(
								Locale.ROOT,
								"JDBC type-code [%s (%s)] not known to have a corresponding LOB equivalent",
								jdbcTypeCode,
								JdbcTypeNameMapper.getTypeName( jdbcTypeCode )
						) );
		}
	}

}
