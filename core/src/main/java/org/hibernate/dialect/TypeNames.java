/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.dialect;

import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Iterator;

import org.hibernate.MappingException;
import org.hibernate.util.StringHelper;

/**
 * This class maps a type to names. Associations
 * may be marked with a capacity. Calling the get()
 * method with a type and actual size n will return
 * the associated name with smallest capacity >= n,
 * if available and an unmarked default type otherwise.
 * Eg, setting
 * <pre>
 *	names.put(type,        "TEXT" );
 *	names.put(type,   255, "VARCHAR($l)" );
 *	names.put(type, 65534, "LONGVARCHAR($l)" );
 * </pre>
 * will give you back the following:
 * <pre>
 *  names.get(type)         // --> "TEXT" (default)
 *  names.get(type,    100) // --> "VARCHAR(100)" (100 is in [0:255])
 *  names.get(type,   1000) // --> "LONGVARCHAR(1000)" (1000 is in [256:65534])
 *  names.get(type, 100000) // --> "TEXT" (default)
 * </pre>
 * On the other hand, simply putting
 * <pre>
 *	names.put(type, "VARCHAR($l)" );
 * </pre>
 * would result in
 * <pre>
 *  names.get(type)        // --> "VARCHAR($l)" (will cause trouble)
 *  names.get(type, 100)   // --> "VARCHAR(100)"
 *  names.get(type, 10000) // --> "VARCHAR(10000)"
 * </pre>
 *
 * @author Christoph Beck
 */
public class TypeNames {

	private HashMap weighted = new HashMap();
	private HashMap defaults = new HashMap();

	/**
	 * get default type name for specified type
	 * @param typecode the type key
	 * @return the default type name associated with specified key
	 */
	public String get(int typecode) throws MappingException {
		String result = (String) defaults.get( new Integer(typecode) );
		if (result==null) throw new MappingException("No Dialect mapping for JDBC type: " + typecode);
		return result;
	}

	/**
	 * get type name for specified type and size
	 * @param typecode the type key
	 * @param size the SQL length
	 * @param scale the SQL scale
	 * @param precision the SQL precision
	 * @return the associated name with smallest capacity >= size,
	 * if available and the default type name otherwise
	 */
	public String get(int typecode, int size, int precision, int scale) throws MappingException {
		Map map = (Map) weighted.get( new Integer(typecode) );
		if ( map!=null && map.size()>0 ) {
			// iterate entries ordered by capacity to find first fit
			Iterator entries = map.entrySet().iterator();
			while ( entries.hasNext() ) {
				Map.Entry entry = (Map.Entry)entries.next();
				if ( size <= ( (Integer) entry.getKey() ).intValue() ) {
					return replace( (String) entry.getValue(), size, precision, scale );
				}
			}
		}
		return replace( get(typecode), size, precision, scale );
	}
	
	private static String replace(String type, int size, int precision, int scale) {
		type = StringHelper.replaceOnce(type, "$s", Integer.toString(scale) );
		type = StringHelper.replaceOnce(type, "$l", Integer.toString(size) );
		return StringHelper.replaceOnce(type, "$p", Integer.toString(precision) );
	}

	/**
	 * set a type name for specified type key and capacity
	 * @param typecode the type key
	 */
	public void put(int typecode, int capacity, String value) {
		TreeMap map = (TreeMap)weighted.get( new Integer(typecode) );
		if (map == null) {// add new ordered map
			map = new TreeMap();
			weighted.put( new Integer(typecode), map );
		}
		map.put(new Integer(capacity), value);
	}

	/**
	 * set a default type name for specified type key
	 * @param typecode the type key
	 */
	public void put(int typecode, String value) {
		defaults.put( new Integer(typecode), value );
	}
}






