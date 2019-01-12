/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.spi;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.internal.CoreMessageLogger;

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * (Badly named) helper for dealing with standard JDBC types as defined by {@link java.sql.Types}
 *
 * @author Steve Ebersole
 */
public final class JdbcTypeNameMapper {
	private static final CoreMessageLogger LOG = messageLogger( JdbcTypeNameMapper.class );

	private static Map<String,Integer> NAME_TO_CODE;
	private static Map<Integer,String> CODE_TO_NAME;

	static {
		final Map<String,Integer> nameToCodeMap = new HashMap<>();
		final Map<Integer,String> codeToNameMap = new HashMap<>();

		final Field[] fields = java.sql.Types.class.getFields();
		for ( Field field : fields ) {
			try {
				final String name = field.getName();
				final int code = field.getInt( null );

				final String old = codeToNameMap.put( code, name );
				if ( old != null ) {
					LOG.JavaSqlTypesMappedSameCodeMultipleTimes( code, old, name );
				}

				nameToCodeMap.put( name, code );
			}
			catch (IllegalAccessException e) {
				throw new HibernateException( "Unable to access JDBC type mapping [" + field.getName() + "]", e );
			}
		}

		NAME_TO_CODE = Collections.unmodifiableMap( nameToCodeMap );
		CODE_TO_NAME = Collections.unmodifiableMap( codeToNameMap );
	}

	private JdbcTypeNameMapper() {
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
		return CODE_TO_NAME.containsKey( typeCode );
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
		String name = CODE_TO_NAME.get( typeCode );
		if ( name == null ) {
			return "UNKNOWN(" + typeCode + ")";
		}
		return name;
	}

	public static Integer getTypeCode(String name) {
		return NAME_TO_CODE.get( name );
	}
}
