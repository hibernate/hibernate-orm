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
 * Represents a flushing strategy. The flush process synchronizes
 * database state with session state by detecting state changes
 * and executing SQL statements.
 *
 * @see Session#setFlushMode(FlushMode)
 * @see Query#setFlushMode(FlushMode)
 * @see Criteria#setFlushMode(FlushMode)
 *
 * @author Gavin King
 */
public final class FlushMode implements Serializable {
	private static final Map INSTANCES = new HashMap();

	private final int level;
	private final String name;

	private FlushMode(int level, String name) {
		this.level = level;
		this.name = name;
	}

	public String toString() {
		return name;
	}

	/**
	 * The {@link Session} is never flushed unless {@link Session#flush}
	 * is explicitly called by the application. This mode is very
	 * efficient for read only transactions.
	 *
	 * @deprecated use {@link #MANUAL} instead.
	 */
	public static final FlushMode NEVER = new FlushMode( 0, "NEVER" );

	/**
	 * The {@link Session} is only ever flushed when {@link Session#flush}
	 * is explicitly called by the application. This mode is very
	 * efficient for read only transactions.
	 */
	public static final FlushMode MANUAL = new FlushMode( 0, "MANUAL" );

	/**
	 * The {@link Session} is flushed when {@link Transaction#commit}
	 * is called.
	 */
	public static final FlushMode COMMIT = new FlushMode(5, "COMMIT");

	/**
	 * The {@link Session} is sometimes flushed before query execution
	 * in order to ensure that queries never return stale state. This
	 * is the default flush mode.
	 */
	public static final FlushMode AUTO = new FlushMode(10, "AUTO");

	/**
	 * The {@link Session} is flushed before every query. This is
	 * almost always unnecessary and inefficient.
	 */
	public static final FlushMode ALWAYS = new FlushMode(20, "ALWAYS");
	
	public boolean lessThan(FlushMode other) {
		return this.level<other.level;
	}

	static {
		INSTANCES.put( NEVER.name, NEVER );
		INSTANCES.put( MANUAL.name, MANUAL );
		INSTANCES.put( AUTO.name, AUTO );
		INSTANCES.put( ALWAYS.name, ALWAYS );
		INSTANCES.put( COMMIT.name, COMMIT );
	}

	public static boolean isManualFlushMode(FlushMode mode) {
		return MANUAL.level == mode.level;
	}

	private Object readResolve() {
		return INSTANCES.get( name );
	}

	public static FlushMode parse(String name) {
		return ( FlushMode ) INSTANCES.get( name );
	}
}
