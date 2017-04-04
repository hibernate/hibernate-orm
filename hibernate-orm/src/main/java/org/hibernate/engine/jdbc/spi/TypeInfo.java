/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.spi;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.ArrayHelper;

import org.jboss.logging.Logger;

/**
 * Models type info extracted from {@link java.sql.DatabaseMetaData#getTypeInfo()}
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("UnusedDeclaration")
public class TypeInfo {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			TypeInfo.class.getName()
	);

	private final String typeName;
	private final int jdbcTypeCode;
	private final String[] createParams;
	private final boolean unsigned;
	private final int precision;
	private final short minimumScale;
	private final short maximumScale;
	private final boolean fixedPrecisionScale;
	private final String literalPrefix;
	private final String literalSuffix;
	private final boolean caseSensitive;
	private final TypeSearchability searchability;
	private final TypeNullability nullability;

	private TypeInfo(
			String typeName,
			int jdbcTypeCode,
			String[] createParams,
			boolean unsigned,
			int precision,
			short minimumScale,
			short maximumScale,
			boolean fixedPrecisionScale,
			String literalPrefix,
			String literalSuffix,
			boolean caseSensitive,
			TypeSearchability searchability,
			TypeNullability nullability) {
		this.typeName = typeName;
		this.jdbcTypeCode = jdbcTypeCode;
		this.createParams = createParams;
		this.unsigned = unsigned;
		this.precision = precision;
		this.minimumScale = minimumScale;
		this.maximumScale = maximumScale;
		this.fixedPrecisionScale = fixedPrecisionScale;
		this.literalPrefix = literalPrefix;
		this.literalSuffix = literalSuffix;
		this.caseSensitive = caseSensitive;
		this.searchability = searchability;
		this.nullability = nullability;
	}

	/**
	 * Extract the type information from the JDBC driver's DatabaseMetaData
	 *
	 * @param metaData The JDBC metadata
	 *
	 * @return The extracted type info
	 */
	public static LinkedHashSet<TypeInfo> extractTypeInfo(DatabaseMetaData metaData) {
		final LinkedHashSet<TypeInfo> typeInfoSet = new LinkedHashSet<TypeInfo>();
		try {
			final ResultSet resultSet = metaData.getTypeInfo();
			try {
				while ( resultSet.next() ) {
					typeInfoSet.add(
							new TypeInfo(
									resultSet.getString( "TYPE_NAME" ),
									resultSet.getInt( "DATA_TYPE" ),
									interpretCreateParams( resultSet.getString( "CREATE_PARAMS" ) ),
									resultSet.getBoolean( "UNSIGNED_ATTRIBUTE" ),
									resultSet.getInt( "PRECISION" ),
									resultSet.getShort( "MINIMUM_SCALE" ),
									resultSet.getShort( "MAXIMUM_SCALE" ),
									resultSet.getBoolean( "FIXED_PREC_SCALE" ),
									resultSet.getString( "LITERAL_PREFIX" ),
									resultSet.getString( "LITERAL_SUFFIX" ),
									resultSet.getBoolean( "CASE_SENSITIVE" ),
									TypeSearchability.interpret( resultSet.getShort( "SEARCHABLE" ) ),
									TypeNullability.interpret( resultSet.getShort( "NULLABLE" ) )
							)
					);
				}
			}
			catch ( SQLException e ) {
				LOG.unableToAccessTypeInfoResultSet( e.toString() );
			}
			finally {
				try {
					resultSet.close();
				}
				catch ( SQLException e ) {
					LOG.unableToReleaseTypeInfoResultSet();
				}
			}
		}
		catch ( SQLException e ) {
			LOG.unableToRetrieveTypeInfoResultSet( e.toString() );
		}

		return typeInfoSet;
	}

	private static String[] interpretCreateParams(String value) {
		if ( value == null || value.length() == 0 ) {
			return ArrayHelper.EMPTY_STRING_ARRAY;
		}
		return value.split( "," );
	}

	public String getTypeName() {
		return typeName;
	}

	public int getJdbcTypeCode() {
		return jdbcTypeCode;
	}

	public String[] getCreateParams() {
		return createParams;
	}

	public boolean isUnsigned() {
		return unsigned;
	}

	public int getPrecision() {
		return precision;
	}

	public short getMinimumScale() {
		return minimumScale;
	}

	public short getMaximumScale() {
		return maximumScale;
	}

	public boolean isFixedPrecisionScale() {
		return fixedPrecisionScale;
	}

	public String getLiteralPrefix() {
		return literalPrefix;
	}

	public String getLiteralSuffix() {
		return literalSuffix;
	}

	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public TypeSearchability getSearchability() {
		return searchability;
	}

	public TypeNullability getNullability() {
		return nullability;
	}
}
