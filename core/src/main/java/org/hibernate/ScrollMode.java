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
package org.hibernate;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Specifies the type of JDBC scrollable result set to use
 * underneath a <tt>ScrollableResults</tt>
 *
 * @see Query#scroll(ScrollMode)
 * @see ScrollableResults
 * @author Gavin King
 */
public final class ScrollMode implements Serializable {
	private final int resultSetType;
	private final String name;
	private static final Map INSTANCES = new HashMap();

	private ScrollMode(int level, String name) {
		this.resultSetType=level;
		this.name=name;
	}
	
	public String toString() {
		return name;
	}
	
	/**
	 * @return the JDBC result set type code
	 */
	public int toResultSetType() {
		return resultSetType;
	}
	
	/**
	 * @see java.sql.ResultSet.TYPE_FORWARD_ONLY
	 */
	public static final ScrollMode FORWARD_ONLY = new ScrollMode(ResultSet.TYPE_FORWARD_ONLY, "FORWARD_ONLY");
	/**
	 * @see java.sql.ResultSet.TYPE_SCROLL_SENSITIVE
	 */
	public static final ScrollMode SCROLL_SENSITIVE = new ScrollMode(ResultSet.TYPE_SCROLL_SENSITIVE, "SCROLL_SENSITIVE");
	/**
	 * Note that since the Hibernate session acts as a cache, you
	 * might need to expicitly evict objects, if you need to see
	 * changes made by other transactions. 
	 * @see java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE
	 */
	public static final ScrollMode SCROLL_INSENSITIVE = new ScrollMode(ResultSet.TYPE_SCROLL_INSENSITIVE, "SCROLL_INSENSITIVE");
	
	public boolean lessThan(ScrollMode other) {
		return this.resultSetType<other.resultSetType;
	}

	static {
		INSTANCES.put( FORWARD_ONLY.name, FORWARD_ONLY );
		INSTANCES.put( SCROLL_INSENSITIVE.name, SCROLL_INSENSITIVE );
		INSTANCES.put( SCROLL_SENSITIVE.name, SCROLL_SENSITIVE );
	}

	private Object readResolve() {
		return INSTANCES.get(name);
	}

}






