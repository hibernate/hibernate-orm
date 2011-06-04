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
public enum FlushMode {
		/**
	 * The {@link Session} is never flushed unless {@link Session#flush}
	 * is explicitly called by the application. This mode is very
	 * efficient for read only transactions.
	 *
	 * @deprecated use {@link #MANUAL} instead.
	 */
	NEVER ( 0 ),

	/**
	 * The {@link Session} is only ever flushed when {@link Session#flush}
	 * is explicitly called by the application. This mode is very
	 * efficient for read only transactions.
	 */
	MANUAL( 0 ),

	/**
	 * The {@link Session} is flushed when {@link Transaction#commit}
	 * is called.
	 */
	COMMIT(5 ),

	/**
	 * The {@link Session} is sometimes flushed before query execution
	 * in order to ensure that queries never return stale state. This
	 * is the default flush mode.
	 */
	AUTO(10 ),

	/**
	 * The {@link Session} is flushed before every query. This is
	 * almost always unnecessary and inefficient.
	 */
	ALWAYS(20 );

	private final int level;

	private FlushMode(int level) {
		this.level = level;
	}
	
	public boolean lessThan(FlushMode other) {
		return this.level<other.level;
	}

	public static boolean isManualFlushMode(FlushMode mode) {
		return MANUAL.level == mode.level;
	}
}
