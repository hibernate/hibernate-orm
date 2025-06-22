/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor;

import java.lang.reflect.Field;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.type.SqlTypes;

import static java.util.Collections.unmodifiableMap;
import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * (Badly named) helper for dealing with standard JDBC types as defined by {@link java.sql.Types}
 *
 * @author Steve Ebersole
 */
public final class JdbcTypeNameMapper {
	private static final CoreMessageLogger LOG = messageLogger( JdbcTypeNameMapper.class );

	private static final Map<Integer, String> JDBC_TYPE_MAP = buildJdbcTypeMap( Types.class );
	private static final Map<Integer, String> SQL_TYPE_MAP = buildJdbcTypeMap( SqlTypes.class );
	private static final Map<String, Integer> SQL_TYPE_NAME_MAP = buildJdbcTypeNameMap( SqlTypes.class );

	private static Map<Integer, String> buildJdbcTypeMap(Class<?> typesClass) {
		final HashMap<Integer, String> map = new HashMap<>();
		for ( Field field : typesClass.getFields() ) {
			try {
				final int code = field.getInt( null );
				final String old = map.put( code, field.getName() );
				if ( old != null ) {
					LOG.JavaSqlTypesMappedSameCodeMultipleTimes( code, old, field.getName() );
				}
			}
			catch ( IllegalAccessException e ) {
				throw new HibernateException( "Unable to access JDBC type mapping [" + field.getName() + "]", e );
			}
		}
		return unmodifiableMap( map );
	}

	private static Map<String, Integer> buildJdbcTypeNameMap(Class<?> typesClass) {
		final HashMap<String, Integer> map = new HashMap<>();
		for ( Field field : typesClass.getFields() ) {
			try {
				final int code = field.getInt( null );
				map.put( field.getName(), code );
			}
			catch ( IllegalAccessException e ) {
				throw new HibernateException( "Unable to access JDBC type mapping [" + field.getName() + "]", e );
			}
		}
		return unmodifiableMap( map );
	}

	/**
	 * Determine whether the given JDBC type code represents a standard JDBC type
	 * ("standard" being those defined on {@link java.sql.Types}).
	 *
	 * @implNote {@link java.sql.Types#OTHER} is also "filtered out" as being non-standard.
	 *
	 * @param typeCode The JDBC type code to check
	 *
	 * @return {@code true} to indicate the type code is a standard type code; {@code false} otherwise.
	 */
	public static boolean isStandardTypeCode(int typeCode) {
		return isStandardTypeCode( Integer.valueOf( typeCode ) );
	}

	/**
	 * Same as call to {@link #isStandardTypeCode(int)}
	 *
	 * @see #isStandardTypeCode(int)
	 */
	public static boolean isStandardTypeCode(Integer typeCode) {
		return JDBC_TYPE_MAP.containsKey( typeCode );
	}

	/**
	 * Get the type name as in the static field names defined on {@link java.sql.Types}.
	 * If a type code is not recognized, it is reported as {@code UNKNOWN(?)} where '?'
	 * is replaced with the given type code.
	 *
	 * @apiNote Useful for logging.
	 *
	 * @param typeCode The type code to find the name for.
	 *
	 * @return The type name.
	 */
	public static String getTypeName(Integer typeCode) {
		final String name = SQL_TYPE_MAP.get( typeCode );
		return name == null ? "UNKNOWN(" + typeCode + ")" : name;
	}

	/**
	 * Get the type code as in the static field names defined on {@link java.sql.Types}.
	 * If a type name is not recognized, <code>null</code> is returned.
	 *
	 * @param typeName The type name to find the code for.
	 *
	 * @return The type code.
	 */
	public static Integer getTypeCode(String typeName) {
		return SQL_TYPE_NAME_MAP.get( typeName );
	}

	private JdbcTypeNameMapper() {
	}

}
