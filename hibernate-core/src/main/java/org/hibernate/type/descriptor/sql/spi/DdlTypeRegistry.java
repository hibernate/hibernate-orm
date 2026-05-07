/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.sql.spi;

import java.io.Serializable;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.JdbcTypeNameMapper;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.spi.TypeConfiguration;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static org.hibernate.type.descriptor.JdbcTypeNameMapper.isStandardTypeCode;


/**
 * A registry mapping {@link org.hibernate.type.SqlTypes JDBC type codes}
 * to instances of the {@link DdlType} interface.
 *
 * @author Christian Beikov
 *
 * @since 6.0
 */
public class DdlTypeRegistry implements Serializable {
//	private static final Logger LOG = Logger.getLogger( DdlTypeRegistry.class );

	private final Map<Integer, DdlType> ddlTypes = new HashMap<>();
	private final Map<String, Integer> sqlTypes = new TreeMap<>( CASE_INSENSITIVE_ORDER );

	public DdlTypeRegistry(TypeConfiguration typeConfiguration) {
//		this.typeConfiguration = typeConfiguration;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// baseline descriptors

	/**
	 * Add a mapping from the {@linkplain DdlType#getSqlTypeCode() type code}
	 * of the given {@link DdlType} to the given {@code DdlType}.
	 */
	public void addDescriptor(DdlType ddlType) {
		addDescriptor( ddlType.getSqlTypeCode(), ddlType );
	}

	/**
	 * Add a mapping from the given type code to the given {@link DdlType}.
	 */
	public void addDescriptor(int sqlTypeCode, DdlType ddlType) {
		final var previous = ddlTypes.put( sqlTypeCode, ddlType );
		if ( previous != null && previous != ddlType ) {
			for ( String rawTypeName : previous.getRawTypeNames() ) {
				sqlTypes.remove( rawTypeName );
			}
//			LOG.tracef( "addDescriptor(%d, %s) replaced previous registration(%s)", sqlTypeCode, ddlType, previous );
		}
		addSqlType( ddlType, sqlTypeCode );
	}

	/**
	 * Add a mapping from the {@linkplain DdlType#getSqlTypeCode() type code}
	 * of the given {@link DdlType} to the given {@code DdlType}, if there
	 * is no mapping already present for that type code.
	 */
	public void addDescriptorIfAbsent(DdlType ddlType) {
		addDescriptorIfAbsent( ddlType.getSqlTypeCode(), ddlType );
	}

	/**
	 * Add a mapping from the given type code to the given {@link DdlType},
	 * if there is no mapping already present for the given type code.
	 */
	public void addDescriptorIfAbsent(int sqlTypeCode, DdlType ddlType) {
		if ( ddlTypes.putIfAbsent( sqlTypeCode, ddlType ) == null ) {
			addSqlType( ddlType, sqlTypeCode );
		}
	}

	/**
	 * Add a mapping from the given type code to the raw type name of the
	 * given {@link DdlType}.
	 */
	private void addSqlType(DdlType ddlType, int sqlTypeCode) {
		for ( String rawTypeName : ddlType.getRawTypeNames() ) {
			final Integer previousSqlTypeCode = sqlTypes.put( rawTypeName, sqlTypeCode );
			// Prefer the standard code over a custom code for a certain type name
			if ( previousSqlTypeCode != null
					&& isStandardTypeCode( previousSqlTypeCode )
					&& ( !isStandardTypeCode( sqlTypeCode ) || isBigger( previousSqlTypeCode, sqlTypeCode ) ) ) {
				sqlTypes.put( rawTypeName, previousSqlTypeCode );
			}
		}
	}

	/**
	 * Whether {@code typeCode1} is bigger than {@code typeCode2}.
	 * For example, a BIGINT is bigger than INTEGER, but if types are "unrelated",
	 * e.g. VARCHAR and BIGINT, then this method will always return {@code false}.
	 */
	private static boolean isBigger(int typeCode1, int typeCode2) {
		return switch ( typeCode1 ) {
			// Integer type hierarchy: TINYINT < SMALLINT < INTEGER < BIGINT
			case SqlTypes.BIGINT -> typeCode2 == SqlTypes.INTEGER
					|| typeCode2 == SqlTypes.SMALLINT
					|| typeCode2 == SqlTypes.TINYINT;
			case SqlTypes.INTEGER -> typeCode2 == SqlTypes.SMALLINT
					|| typeCode2 == SqlTypes.TINYINT;
			case SqlTypes.SMALLINT -> typeCode2 == SqlTypes.TINYINT;
			// Floating point hierarchy: REAL/FLOAT < DOUBLE
			case SqlTypes.DOUBLE -> typeCode2 == SqlTypes.REAL
					|| typeCode2 == SqlTypes.FLOAT;
			// Character type hierarchy: CHAR < VARCHAR < LONG32VARCHAR < CLOB
			case SqlTypes.CLOB -> typeCode2 == SqlTypes.LONG32VARCHAR
					|| typeCode2 == SqlTypes.VARCHAR
					|| typeCode2 == SqlTypes.CHAR;
			case SqlTypes.LONG32VARCHAR -> typeCode2 == SqlTypes.VARCHAR
					|| typeCode2 == SqlTypes.CHAR;
			case SqlTypes.VARCHAR -> typeCode2 == SqlTypes.CHAR;
			// National character type hierarchy: NCHAR < NVARCHAR < LONG32NVARCHAR < NCLOB
			case SqlTypes.NCLOB -> typeCode2 == SqlTypes.LONG32NVARCHAR
					|| typeCode2 == SqlTypes.NVARCHAR
					|| typeCode2 == SqlTypes.NCHAR;
			case SqlTypes.LONG32NVARCHAR -> typeCode2 == SqlTypes.NVARCHAR
					|| typeCode2 == SqlTypes.NCHAR;
			case SqlTypes.NVARCHAR -> typeCode2 == SqlTypes.NCHAR;
			// Binary type hierarchy: BINARY < VARBINARY < LONG32VARBINARY < BLOB
			case SqlTypes.BLOB -> typeCode2 == SqlTypes.LONG32VARBINARY
					|| typeCode2 == SqlTypes.VARBINARY
					|| typeCode2 == SqlTypes.BINARY;
			case SqlTypes.LONG32VARBINARY -> typeCode2 == SqlTypes.VARBINARY
					|| typeCode2 == SqlTypes.BINARY;
			case SqlTypes.VARBINARY -> typeCode2 == SqlTypes.BINARY;
			// Temporal type hierarchy: DATE/TIME < TIMESTAMP < TIMESTAMP_WITH_TIMEZONE
			case SqlTypes.TIMESTAMP_WITH_TIMEZONE -> typeCode2 == SqlTypes.TIMESTAMP
					|| typeCode2 == SqlTypes.DATE
					|| typeCode2 == SqlTypes.TIME;
			case SqlTypes.TIMESTAMP -> typeCode2 == SqlTypes.DATE
					|| typeCode2 == SqlTypes.TIME;
			default -> false;
		};
	}

	/**
	 * Returns the {@link SqlTypes} type code for the given DDL raw type name, or
	 * {@code null} if the type code cannot be determined from the registrations.
	 */
	public Integer getSqlTypeCode(String rawTypeName) {
		return sqlTypes.get( rawTypeName );
	}

	/**
	 * Returns the registered {@link DdlType} for the given SQL type code.
	 * <p>
	 * Note that the "long" types {@link Types#LONGVARCHAR}, {@link Types#LONGNVARCHAR},
	 * and {@link Types#LONGVARBINARY} are considered synonyms for their non-{@code LONG}
	 * counterparts, with the only difference being that a different default length is
	 * used by default: {@link org.hibernate.Length#LONG} instead of
	 * {@link org.hibernate.Length#DEFAULT}.
	 *
	 */
	public DdlType getDescriptor(int sqlTypeCode) {
		final var ddlType = ddlTypes.get( sqlTypeCode );
		if ( ddlType == null ) {
			return switch ( sqlTypeCode ) {
				// these are no longer considered separate column types as such;
				// they're just used to indicate that JavaType.getLongSqlLength()
				// should be used by default (and that's already handled by the
				// time we get to here)
				case SqlTypes.LONGVARCHAR -> ddlTypes.get( SqlTypes.VARCHAR );
				case SqlTypes.LONGNVARCHAR -> ddlTypes.get( SqlTypes.NVARCHAR );
				case SqlTypes.LONGVARBINARY -> ddlTypes.get( SqlTypes.VARBINARY );
				default -> null;
			};
		}
		return ddlType;
	}

	/**
	 * Get the SQL type name for the specified {@linkplain java.sql.Types JDBC
	 * type code}, filling in the placemarkers {@code $l}, {@code $p}, and {@code $s}
	 * with the default length, precision, and scale for the given SQL dialect.
	 *
	 * @param typeCode the JDBC type code
	 * @param dialect the dialect which determines the default length, precision, and scale
	 * @return a SQL column type
	 */
	public String getTypeName(int typeCode, Dialect dialect) {
		// explicitly enforce dialect's default precisions
		return switch ( typeCode ) {
			case SqlTypes.CHAR, SqlTypes.NCHAR, SqlTypes.VARCHAR, SqlTypes.NVARCHAR, SqlTypes.VARBINARY
					-> getTypeName( typeCode, Size.length( Size.DEFAULT_LENGTH ) );
			case SqlTypes.DECIMAL, SqlTypes.NUMERIC
					-> getTypeName( typeCode, Size.precision( dialect.getDefaultDecimalPrecision() ) );
			case SqlTypes.FLOAT, SqlTypes.REAL
					-> getTypeName( typeCode, Size.precision( dialect.getFloatPrecision() ) );
			case SqlTypes.DOUBLE
					-> getTypeName( typeCode, Size.precision( dialect.getDoublePrecision() ) );
			case SqlTypes.TIMESTAMP, SqlTypes.TIMESTAMP_WITH_TIMEZONE, SqlTypes.TIMESTAMP_UTC
					-> getTypeName( typeCode, Size.precision( dialect.getDefaultTimestampPrecision() ) );
			default -> getTypeName( typeCode, Size.nil() );
		};
	}

	/**
	 * Get the SQL type name for the specified {@link java.sql.Types JDBC type code}
	 * and size, filling in the placemarkers {@code $l}, {@code $p}, and {@code $s}
	 * with the length, precision, and scale determined by the given {@linkplain Size
	 * size object}. The returned type name should be of a SQL type large enough to
	 * accommodate values of the specified size.
	 *
	 * @apiNote Not appropriate for named enum or array types,
	 *          use {@link #getTypeName(int, Size, Type)} instead
	 *
	 * @param typeCode the JDBC type code
	 * @param size an object which determines the length, precision, and scale
	 *
	 * @return the associated type name with the smallest capacity that accommodates
	 *         the given size, if available, and the default type name otherwise
	 */
	private String getTypeName(int typeCode, Size size) {
		final var descriptor = getDescriptor( typeCode );
		if ( descriptor == null ) {
			throw new HibernateException(
					String.format(
							"No type mapping for org.hibernate.type.SqlTypes code: %s (%s)",
							typeCode,
							JdbcTypeNameMapper.getTypeName( typeCode )
					)
			);
		}
		return descriptor.getTypeName( size, null, this );
	}

	/**
	 * Get the SQL type name for the specified {@link java.sql.Types JDBC type code}
	 * and size, filling in the placemarkers {@code $l}, {@code $p}, and {@code $s}
	 * with the length, precision, and scale determined by the given {@linkplain Size
	 * size object}. The returned type name should be of a SQL type large enough to
	 * accommodate values of the specified size.
	 *
	 * @param typeCode the JDBC type code
	 * @param columnSize an object which determines the length, precision, and scale
	 * @param type the {@link Type} mapped to the column
	 *
	 * @return the associated type name with the smallest capacity that accommodates
	 *         the given size, if available, and the default type name otherwise
	 *
	 * @since 6.3
	 */
	public String getTypeName(int typeCode, Size columnSize, Type type) {
		final var descriptor = getDescriptor( typeCode );
		if ( descriptor == null ) {
			throw new HibernateException(
					String.format(
							"No type mapping for org.hibernate.type.SqlTypes code: %s (%s)",
							typeCode,
							JdbcTypeNameMapper.getTypeName( typeCode )
					)
			);
		}
		return descriptor.getTypeName( columnSize, type, this );
	}

	/**
	 * Returns the first raw SQL type name registered for the given JDBC type code.
	 */
	public String getRawTypeName(int typeCode) {
		final var rawTypeNames = getDescriptor( typeCode ).getRawTypeNames();
		if ( rawTypeNames.length == 0 ) {
			throw new HibernateException(
					String.format(
							"No raw type name mapping for org.hibernate.type.SqlTypes code: %s (%s)",
							typeCode,
							JdbcTypeNameMapper.getTypeName( typeCode )
					)
			);
		}
		return rawTypeNames[0];
	}

	/**
	 * Determines if there is a registered {@link DdlType} whose raw type name
	 * matches the given type name, taking into account DDL types registered by
	 * Hibernate.
	 *
	 * @param typeName the type name.
	 *
	 * @return {@code true} if there is a DDL type with the given raw type name
	 */
	public boolean isTypeNameRegistered(final String typeName) {
		return sqlTypes.containsKey( typeName );
	}
}
