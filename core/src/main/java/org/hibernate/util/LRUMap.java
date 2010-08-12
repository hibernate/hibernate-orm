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
package org.hibernate.util;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A simple LRU cache that implements the <code>Map</code> interface. Instances
 * are not thread-safe and should be synchronized externally, for instance by
 * using {@link java.util.Collections#synchronizedMap}.
 * 
 * @author Manuel Dominguez Sarmiento
 */
public class LRUMap extends LinkedHashMap implements Serializable {
	private static final long serialVersionUID = -5522608033020688048L;

	private final int maxEntries;

	public LRUMap(int maxEntries) {
		super( maxEntries, .75f, true );
		this.maxEntries = maxEntries;
	}

	protected boolean removeEldestEntry(Map.Entry eldest) {
		return ( size() > maxEntries );
	}
}