/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.internal.util.StringHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class maintains a mapping of {@linkplain java.sql.Types JDBC type codes} to
 * SQL type names for a {@linkplain Dialect dialect} of SQL. An association between a
 * type code and a SQL type name may be {@linkplain #put(int, long, String) registered}
 * with a <em>capacity</em>, that is, with the maximum size that the given SQL type can
 * accommodate.
 * <p>
 * When a type association is {@linkplain #get(int, Long, Integer, Integer) retrieved}
 * for a given type code and actual size n, {@code get()} will return the associated
 * type name with the smallest capacity greater than or equal to n, if available, or an
 * unmarked default type otherwise.
 * <p>
 * For example, setting:
 *
 * <pre>
 *	names.put( type,        "TEXT" );
 *	names.put( type,   255, "VARCHAR($l)" );
 *	names.put( type, 65534, "LONGVARCHAR($l)" );
 * </pre>
 *
 * will give you back the following:
 *
 * <pre>
 *  names.get( type )         // --> "TEXT" (default)
 *  names.get( type,    100 ) // --> "VARCHAR(100)" (100 is in [0:255])
 *  names.get( type,   1000 ) // --> "LONGVARCHAR(1000)" (1000 is in [256:65534])
 *  names.get( type, 100000 ) // --> "TEXT" (default)
 * </pre>
 *
 * On the other hand, simply putting:
 *
 * <pre>
 *	names.put( type, "VARCHAR($l)" );
 * </pre>
 *
 * would result in:
 *
 * <pre>
 *  names.get( type )        // --> "VARCHAR($l)" (will cause trouble)
 *  names.get( type, 100 )   // --> "VARCHAR(100)"
 *  names.get( type, 10000 ) // --> "VARCHAR(10000)"
 * </pre>
 *
 * Registered type names may contain the placemarkers {@code $l}, {@code $p},
 * and {@code $s}, which will be replaced by the length, precision, and size
 * passed to {@link #get(int, Long, Integer, Integer)}.
 *
 * @author Christoph Beck
 */
public final class TypeNames {
	/**
	 * Holds default type mappings for a JDBC type code. These are the mappings
	 * with no specified maximum size.
	 */
	private final Map<Integer, String> defaults = new HashMap<>();

	/**
	 * Holds mappings which are limited by a maximum size. The nested map is a
	 * {@link TreeMap} with its mappings sorted by maximum size, ensuring proper
	 * iteration ordering in {@link #get(int, Long, Integer, Integer)}
	 */
	private final Map<Integer, Map<Long, String>> weighted = new HashMap<>();

	/**
	 * Get default type name for specified {@link java.sql.Types JDBC type code}.
	 * Does not fill in any placemarkers.
	 *
	 * @param typeCode the JDBC type code
	 *
	 * @return the default type name associated with specified key, or
	 *         null if there was no type name associated with the key
	 */
	public String get(final int typeCode) {
		return defaults.get( typeCode );
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
	public String get(int typeCode, Long size, Integer precision, Integer scale) {
		if ( size != null ) {
			final Map<Long, String> map = weighted.get( typeCode );
			if ( map != null && map.size() > 0 ) {
				// iterate entries ordered by capacity to find first fit
				for ( Map.Entry<Long, String> entry : map.entrySet() ) {
					if ( size <= entry.getKey() ) {
						return replace( entry.getValue(), size, precision, scale );
					}
				}
			}
		}

		// if we get here one of 2 things happened:
		//		1) There was no weighted registration for that typeCode
		//		2) There was no weighting whose max capacity was big enough to contain size
		return replace( get( typeCode ), size, precision, scale );
	}

	/**
	 * Fill in the placemarkers with the given length, precision, and scale.
	 */
	private static String replace(String type, Long size, Integer precision, Integer scale) {
		if ( scale != null ) {
			type = StringHelper.replaceOnce( type, "$s", scale.toString() );
		}
		if ( size != null ) {
			type = StringHelper.replaceOnce( type, "$l", size.toString() );
		}
		if ( precision != null ) {
			type = StringHelper.replaceOnce( type, "$p", precision.toString() );
		}
		return type;
	}

	/**
	 * Register a mapping from the given JDBC type code to the given SQL type name,
	 * with a specified maximum size.
	 *
	 * @param typeCode the JDBC type code
	 * @param capacity The capacity for this weighting
	 * @param value The mapping (type name)
	 */
	public void put(int typeCode, long capacity, String value) {
		weighted.computeIfAbsent( typeCode, k -> new TreeMap<>() )
				.put( capacity, value );
	}

	/**
	 * Register a mapping from the given JDBC type code to the given SQL type name,
	 * with no specified maximum size.
	 *
	 * @param typeCode the JDBC type code
	 * @param value The mapping (type name)
	 */
	public void put(int typeCode, String value) {
		defaults.put( typeCode, value );
	}

	/**
	 * Check whether or not the provided typeName exists.
	 *
	 * @param typeName the type name.
	 *
	 * @return true if the given string has been registered as a type.
	 */
	public boolean containsTypeName(final String typeName) {
		return this.defaults.containsValue( typeName );
	}
}
