/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.spi;

import java.io.Serializable;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.JdbcTypeNameMapper;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * Basically a map from SQL type code (int) -> {@link DdlType}
 *
 * @author Christian Beikov
 *
 * @since 6.0
 */
public class DdlTypeRegistry implements Serializable {
	private static final Logger log = Logger.getLogger( DdlTypeRegistry.class );

	private final Map<Integer, DdlType> ddlTypes = new HashMap<>();
	private final Map<String, Integer> sqlTypes = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );

	public DdlTypeRegistry(TypeConfiguration typeConfiguration) {
//		this.typeConfiguration = typeConfiguration;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// baseline descriptors

	public void addDescriptor(DdlType ddlType) {
		addDescriptor( ddlType.getSqlTypeCode(), ddlType );
	}

	public void addDescriptor(int sqlTypeCode, DdlType ddlType) {
		final DdlType previous = ddlTypes.put( sqlTypeCode, ddlType );
		if ( previous != null && previous != ddlType ) {
			for ( String rawTypeName : previous.getRawTypeNames() ) {
				sqlTypes.remove( rawTypeName );
			}
			log.debugf( "addDescriptor(%d, %s) replaced previous registration(%s)", sqlTypeCode, ddlType, previous );
		}
		addSqlType( ddlType, sqlTypeCode );
	}

	public void addDescriptorIfAbsent(DdlType ddlType) {
		addDescriptorIfAbsent( ddlType.getSqlTypeCode(), ddlType );
	}

	public void addDescriptorIfAbsent(int sqlTypeCode, DdlType ddlType) {
		if ( ddlTypes.putIfAbsent( sqlTypeCode, ddlType ) == null ) {
			addSqlType( ddlType, sqlTypeCode );
		}
	}

	private void addSqlType(DdlType ddlType, int sqlTypeCode) {
		for ( String rawTypeName : ddlType.getRawTypeNames() ) {
			final Integer previousSqlTypeCode = sqlTypes.put( rawTypeName, sqlTypeCode );
			// Prefer the standard code over a custom code for a certain type name
			if ( previousSqlTypeCode != null && JdbcTypeNameMapper.isStandardTypeCode( previousSqlTypeCode ) ) {
				sqlTypes.put( rawTypeName, previousSqlTypeCode );
			}
		}
	}

	/**
	 * Returns the {@link SqlTypes} type code for the given DDL raw type name, or <code>null</code> if it is unknown.
	 */
	public Integer getSqlTypeCode(String rawTypeName) {
		return sqlTypes.get( rawTypeName );
	}

	/**
	 * Returns the registered {@link DdlType} for the given SQL type code.
	 * <p>
	 * Not that the "long" types {@link Types#LONGVARCHAR}, {@link Types#LONGNVARCHAR}
	 * and {@link Types#LONGVARBINARY} are considered synonyms for their
	 * non-{@code LONG} counterparts, with the only difference being that
	 * a different default length is used: {@link org.hibernate.Length#LONG}
	 * instead of {@link org.hibernate.Length#DEFAULT}.
	 *
	 */
	public DdlType getDescriptor(int sqlTypeCode) {
		final DdlType ddlType = ddlTypes.get( sqlTypeCode );
		if ( ddlType == null ) {
			switch ( sqlTypeCode ) {
				// these are no longer considered separate column types as such
				// they're just used to indicate that JavaType.getLongSqlLength()
				// should be used by default (and that's already handled by the
				// time we get to here)
				case SqlTypes.LONGVARCHAR:
					return ddlTypes.get( SqlTypes.VARCHAR );
				case SqlTypes.LONGNVARCHAR:
					return ddlTypes.get( SqlTypes.NVARCHAR );
				case SqlTypes.LONGVARBINARY:
					return ddlTypes.get( SqlTypes.VARBINARY );
			}
		}
		return ddlType;
	}

	public String getTypeName(int typeCode, Dialect dialect) {
		// explicitly enforce dialect's default precisions
		switch ( typeCode ) {
			case SqlTypes.DECIMAL:
			case SqlTypes.NUMERIC:
				return getTypeName( typeCode, Size.precision( dialect.getDefaultDecimalPrecision() ) );
			case SqlTypes.FLOAT:
			case SqlTypes.REAL:
				return getTypeName( typeCode, Size.precision( dialect.getFloatPrecision() ) );
			case SqlTypes.DOUBLE:
				return getTypeName( typeCode, Size.precision( dialect.getDoublePrecision() ) );
			case SqlTypes.TIMESTAMP:
			case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
			case SqlTypes.TIMESTAMP_UTC:
				return getTypeName( typeCode, Size.precision( dialect.getDefaultTimestampPrecision() ) );
			default:
				return getTypeName( typeCode, Size.nil() );
		}
	}

	public String getTypeName(int typeCode, Size size) {
		return getTypeName( typeCode, size.getLength(), size.getPrecision(), size.getScale() );
	}

	/**
	 * Get the SQL type name for the specified {@link java.sql.Types JDBC type code}
	 * and size, filling in the placemarkers {@code $l}, {@code $p}, and {@code $s}
	 * with the given length, precision, and scale.
	 *
	 * @param typeCode the JDBC type code
	 * @param size the SQL length, if any
	 * @param precision the SQL precision, if any
	 * @param scale the SQL scale, if any
	 *
	 * @return the associated name with smallest capacity >= size, if available and
	 *         the default type name otherwise
	 */
	public String getTypeName(int typeCode, Long size, Integer precision, Integer scale) {
		final DdlType descriptor = getDescriptor( typeCode );
		if ( descriptor == null ) {
			throw new HibernateException(
					String.format(
							"No type mapping for org.hibernate.type.SqlTypes code: %s (%s)",
							typeCode,
							JdbcTypeNameMapper.getTypeName( typeCode )
					)
			);
		}
		return descriptor.getTypeName( size, precision, scale );
	}

	/**
	 * Whether or not the given type name has been registered for this dialect (including both hibernate type names and
	 * custom-registered type names).
	 *
	 * @param typeName the type name.
	 *
	 * @return true if the given string has been registered either as a hibernate type or as a custom-registered one
	 */
	public boolean isTypeNameRegistered(final String typeName) {
		for ( DdlType value : ddlTypes.values() ) {
			if ( value.getRawTypeName().equals( typeName ) ) {
				return true;
			}
		}

		return false;
	}

}
