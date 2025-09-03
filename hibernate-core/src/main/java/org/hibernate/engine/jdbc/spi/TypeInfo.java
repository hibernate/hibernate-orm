/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.spi;

import java.lang.invoke.MethodHandles;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;

import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

import static org.hibernate.internal.util.StringHelper.split;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_STRING_ARRAY;

/**
 * Models type info extracted from {@link DatabaseMetaData#getTypeInfo()}
 *
 * @author Steve Ebersole
 *
 * @deprecated This class is no longer used and will be removed
 */
@Deprecated(since = "7.0", forRemoval = true)
public class TypeInfo {

	private static final CoreMessageLogger log = Logger.getMessageLogger(
			MethodHandles.lookup(),
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
		final LinkedHashSet<TypeInfo> typeInfoSet = new LinkedHashSet<>();
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
				log.unableToAccessTypeInfoResultSet( e.toString() );
			}
			finally {
				try {
					resultSet.close();
				}
				catch ( SQLException e ) {
					log.unableToReleaseTypeInfoResultSet();
				}
			}
		}
		catch ( SQLException e ) {
			log.unableToRetrieveTypeInfoResultSet( e.toString() );
		}

		return typeInfoSet;
	}

	private static String[] interpretCreateParams(String value) {
		return value == null || value.isEmpty()
				? EMPTY_STRING_ARRAY
				: split( ",", value );
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
