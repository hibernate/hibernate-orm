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
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an association fetching strategy. This is used
 * together with the <tt>Criteria</tt> API to specify runtime
 * fetching strategies.<br>
 * <br>
 * For HQL queries, use the <tt>FETCH</tt> keyword instead.
 *
 * @see Criteria#setFetchMode(java.lang.String, FetchMode)
 * @author Gavin King
 */
public final class FetchMode implements Serializable {
	private final String name;
	private static final Map INSTANCES = new HashMap();

	private FetchMode(String name) {
		this.name=name;
	}
	public String toString() {
		return name;
	}
	/**
	 * Default to the setting configured in the mapping file.
	 */
	public static final FetchMode DEFAULT = new FetchMode("DEFAULT");

	/**
	 * Fetch using an outer join. Equivalent to <tt>fetch="join"</tt>.
	 */
	public static final FetchMode JOIN = new FetchMode("JOIN");
	/**
	 * Fetch eagerly, using a separate select. Equivalent to
	 * <tt>fetch="select"</tt>.
	 */
	public static final FetchMode SELECT = new FetchMode("SELECT");

	/**
	 * Fetch lazily. Equivalent to <tt>outer-join="false"</tt>.
	 * @deprecated use <tt>FetchMode.SELECT</tt>
	 */
	public static final FetchMode LAZY = SELECT;
	/**
	 * Fetch eagerly, using an outer join. Equivalent to
	 * <tt>outer-join="true"</tt>.
	 * @deprecated use <tt>FetchMode.JOIN</tt>
	 */
	public static final FetchMode EAGER = JOIN;
	
	static {
		INSTANCES.put( JOIN.name, JOIN );
		INSTANCES.put( SELECT.name, SELECT );
		INSTANCES.put( DEFAULT.name, DEFAULT );
	}

	private Object readResolve() {
		return INSTANCES.get(name);
	}

}





