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
 */
package org.hibernate.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Various help for handling collections.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public final class CollectionHelper {
    public static final int MINIMUM_INITIAL_CAPACITY = 16;
	public static final float LOAD_FACTOR = 0.75f;

	public static final List EMPTY_LIST = Collections.unmodifiableList( new ArrayList(0) );
	public static final Collection EMPTY_COLLECTION = Collections.unmodifiableCollection( new ArrayList(0) );
	public static final Map EMPTY_MAP = Collections.unmodifiableMap( new HashMap(0) );

	private CollectionHelper() {
	}

	/**
	 * Build a properly sized map, especially handling load size and load factor to prevent immediate resizing.
	 * <p/>
	 * Especially helpful for copy map contents.
	 *
	 * @param size The size to make the map.
	 * @return The sized map.
	 */
	public static Map mapOfSize(int size) {
		return new HashMap( determineProperSizing( size ), LOAD_FACTOR );
	}

	/**
	 * Given a map, determine the proper initial size for a new Map to hold the same number of values.
	 * Specifically we want to account for load size and load factor to prevent immediate resizing.
	 *
	 * @param original The original map
	 * @return The proper size.
	 */
	public static int determineProperSizing(Map original) {
		return determineProperSizing( original.size() );
	}

	/**
	 * Given a set, determine the proper initial size for a new set to hold the same number of values.
	 * Specifically we want to account for load size and load factor to prevent immediate resizing.
	 *
	 * @param original The original set
	 * @return The proper size.
	 */
	public static int determineProperSizing(Set original) {
		return determineProperSizing( original.size() );
	}

	/**
	 * Determine the proper initial size for a new collection in order for it to hold the given a number of elements.
	 * Specifically we want to account for load size and load factor to prevent immediate resizing.
	 *
	 * @param numberOfElements The number of elements to be stored.
	 * @return The proper size.
	 */
	public static int determineProperSizing(int numberOfElements) {
		int actual = ( (int) (numberOfElements / LOAD_FACTOR) ) + 1;
		return Math.max( actual, MINIMUM_INITIAL_CAPACITY );
	}
}
