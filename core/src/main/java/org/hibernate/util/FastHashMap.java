//$Id: FastHashMap.java 3890 2004-06-03 16:31:32Z steveebersole $
/*
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

// This class was taken from Apache commons-collections -
// I made it final + removed the "slow" mode...

package org.hibernate.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>A customized implementation of <code>java.util.HashMap</code> designed
 * to operate in a multithreaded environment where the large majority of
 * method calls are read-only, instead of structural changes.
 * Read calls are non-synchronized and write calls perform the
 * following steps:</p>
 * <ul>
 * <li>Clone the existing collection
 * <li>Perform the modification on the clone
 * <li>Replace the existing collection with the (modified) clone
 * </ul>
 * <p><strong>NOTE</strong>: If you are creating and accessing a
 * <code>HashMap</code> only within a single thread, you should use
 * <code>java.util.HashMap</code> directly (with no synchronization), for
 * maximum performance.</p>
 *
 */

public final class FastHashMap implements Map, Serializable {

	// ----------------------------------------------------------- Constructors

	/**
	 * Construct a an empty map.
	 */
	public FastHashMap() {

		super();
		this.map = new HashMap();

	}

	/**
	 * Construct an empty map with the specified capacity.
	 *
	 * @param capacity The initial capacity of the empty map
	 */
	public FastHashMap(int capacity) {

		super();
		this.map = new HashMap(capacity);

	}

	/**
	 * Construct an empty map with the specified capacity and load factor.
	 *
	 * @param capacity The initial capacity of the empty map
	 * @param factor The load factor of the new map
	 */
	public FastHashMap(int capacity, float factor) {

		super();
		this.map = new HashMap(capacity, factor);

	}

	/**
	 * Construct a new map with the same mappings as the specified map.
	 *
	 * @param map The map whose mappings are to be copied
	 */
	public FastHashMap(Map map) {

		super();
		this.map = new HashMap(map);

	}

	// ----------------------------------------------------- Instance Variables

	/**
	 * The underlying map we are managing.
	 */
	private HashMap map = null;

	// --------------------------------------------------------- Public Methods

	/**
	 * Remove all mappings from this map.
	 */
	public void clear() {

		synchronized (this) {
			HashMap temp = (HashMap) map.clone();
			temp.clear();
			map = temp;
		}

	}

	/**
	 * Return a shallow copy of this <code>FastHashMap</code> instance.
	 * The keys and values themselves are not copied.
	 */
	public Object clone() {

		return new FastHashMap(map);

	}

	/**
	 * Return <code>true</code> if this map contains a mapping for the
	 * specified key.
	 *
	 * @param key Key to be searched for
	 */
	public boolean containsKey(Object key) {

		return map.containsKey(key);

	}

	/**
	 * Return <code>true</code> if this map contains one or more keys mapping
	 * to the specified value.
	 *
	 * @param value Value to be searched for
	 */
	public boolean containsValue(Object value) {

		return map.containsValue(value);

	}

	/**
	 * Return a collection view of the mappings contained in this map.  Each
	 * element in the returned collection is a <code>Map.Entry</code>.
	 */
	public Set entrySet() {

		return map.entrySet();

	}

	/**
	 * Compare the specified object with this list for equality.  This
	 * implementation uses exactly the code that is used to define the
	 * list equals function in the documentation for the
	 * <code>Map.equals</code> method.
	 *
	 * @param o Object to be compared to this list
	 */
	public boolean equals(Object o) {

		// Simple tests that require no synchronization
		if (o == this)
			return true;
		else if ( !(o instanceof Map) )
			return false;
		Map mo = (Map) o;

		// Compare the two maps for equality

		if ( mo.size() != map.size() )
			return false;
		java.util.Iterator i = map.entrySet().iterator();
		while ( i.hasNext() ) {
			Map.Entry e = (Map.Entry) i.next();
			Object key = e.getKey();
			Object value = e.getValue();
			if (value == null) {
				if ( !( mo.get(key) == null && mo.containsKey(key) ) )
					return false;
			}
			else {
				if ( !value.equals( mo.get(key) ) )
					return false;
			}
		}
		return true;

	}

	/**
	 * Return the value to which this map maps the specified key.  Returns
	 * <code>null</code> if the map contains no mapping for this key, or if
	 * there is a mapping with a value of <code>null</code>.  Use the
	 * <code>containsKey()</code> method to disambiguate these cases.
	 *
	 * @param key Key whose value is to be returned
	 */
	public Object get(Object key) {

		return map.get(key);

	}

	/**
	 * Return the hash code value for this map.  This implementation uses
	 * exactly the code that is used to define the list hash function in the
	 * documentation for the <code>Map.hashCode</code> method.
	 */
	public int hashCode() {

		int h = 0;
		java.util.Iterator i = map.entrySet().iterator();
		while ( i.hasNext() )
			h += i.next().hashCode();
		return h;

	}

	/**
	 * Return <code>true</code> if this map contains no mappings.
	 */
	public boolean isEmpty() {

		return map.isEmpty();

	}

	/**
	 * Return a set view of the keys contained in this map.
	 */
	public Set keySet() {

		return map.keySet();

	}

	/**
	 * Associate the specified value with the specified key in this map.
	 * If the map previously contained a mapping for this key, the old
	 * value is replaced and returned.
	 *
	 * @param key The key with which the value is to be associated
	 * @param value The value to be associated with this key
	 */
	public Object put(Object key, Object value) {

		synchronized (this) {
			HashMap temp = (HashMap) map.clone();
			Object result = temp.put(key, value);
			map = temp;
			return (result);
		}

	}

	/**
	 * Copy all of the mappings from the specified map to this one, replacing
	 * any mappings with the same keys.
	 *
	 * @param in Map whose mappings are to be copied
	 */
	public void putAll(Map in) {

		synchronized (this) {
			HashMap temp = (HashMap) map.clone();
			temp.putAll(in);
			map = temp;
		}

	}

	/**
	 * Remove any mapping for this key, and return any previously
	 * mapped value.
	 *
	 * @param key Key whose mapping is to be removed
	 */
	public Object remove(Object key) {

		synchronized (this) {
			HashMap temp = (HashMap) map.clone();
			Object result = temp.remove(key);
			map = temp;
			return (result);
		}

	}

	/**
	 * Return the number of key-value mappings in this map.
	 */
	public int size() {

		return map.size();

	}

	/**
	 * Return a collection view of the values contained in this map.
	 */
	public Collection values() {

		return map.values();

	}

	public String toString() { return map.toString(); }

}





