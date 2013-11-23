/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.type.descriptor;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * (Badly named) helper for dealing with standard JDBC types as defined by {@link java.sql.Types}
 *
 * @author Steve Ebersole
 */
public final class JdbcTypeNameMapper {
    private static final CoreMessageLogger LOG = CoreLogging.messageLogger( JdbcTypeNameMapper.class );

	private static Map<Integer,String> JDBC_TYPE_MAP = buildJdbcTypeMap();

	private static Map<Integer, String> buildJdbcTypeMap() {
		HashMap<Integer, String> map = new HashMap<Integer, String>();
		Field[] fields = java.sql.Types.class.getFields();
		if ( fields == null ) {
			throw new HibernateException( "Unexpected problem extracting JDBC type mapping codes from java.sql.Types" );
		}
		for ( Field field : fields ) {
			try {
				final int code = field.getInt( null );
				String old = map.put( code, field.getName() );
                if ( old != null ) {
					LOG.JavaSqlTypesMappedSameCodeMultipleTimes( code, old, field.getName() );
				}
			}
			catch ( IllegalAccessException e ) {
				throw new HibernateException( "Unable to access JDBC type mapping [" + field.getName() + "]", e );
			}
		}
		return Collections.unmodifiableMap( map );
	}

	/**
	 * Determine whether the given JDBC type code represents a standard JDBC type ("standard" being those defined on
	 * {@link java.sql.Types}).
	 *
	 * NOTE : {@link java.sql.Types#OTHER} is also "filtered out" as being non-standard.
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
	 * Get the type name as in the static field names defined on {@link java.sql.Types}.  If a type code is not
	 * recognized, it is reported as {@code UNKNOWN(?)} where '?' is replace with the given type code.
	 *
	 * Intended as useful for logging purposes...
	 *
	 * @param typeCode The type code to find the name for.
	 *
	 * @return The type name.
	 */
	public static String getTypeName(Integer typeCode) {
		String name = JDBC_TYPE_MAP.get( typeCode );
		if ( name == null ) {
			return "UNKNOWN(" + typeCode + ")";
		}
		return name;
	}

	private JdbcTypeNameMapper() {
	}

}
