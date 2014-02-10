/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008 Red Hat Inc. or third-party contributors as
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
package org.hibernate;

import java.util.Locale;

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
	@Deprecated
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

	/**
	 * Checks to see if {@code this} flush mode is less than the given flush mode.
	 *
	 * @param other THe flush mode value to be checked against {@code this}
	 *
	 * @return {@code true} indicates {@code other} is less than {@code this}; {@code false} otherwise
	 */
	public boolean lessThan(FlushMode other) {
		return this.level < other.level;
	}

	/**
	 * Checks to see if the given mode is the same as {@link #MANUAL}.
	 *
	 * @param mode The mode to check
	 *
	 * @return true/false
	 *
	 * @deprecated Just use equality check against {@link #MANUAL}.  Legacy from before this was an enum
	 */
	@Deprecated
	public static boolean isManualFlushMode(FlushMode mode) {
		return MANUAL.level == mode.level;
	}

	public String toExternalForm() {
		return name().toLowerCase( Locale.ENGLISH );
	}

	/**
	 * Interprets an external representation of the flush mode.  {@code null} is returned as {@code null}, otherwise
	 * {@link FlushMode#valueOf(String)} is used with the upper-case version of the incoming value.  An unknown,
	 * non-null value results in a MappingException being thrown.
	 *
	 * @param externalName The external representation
	 *
	 * @return The interpreted FlushMode value.
	 *
	 * @throws MappingException Indicates an unrecognized external representation
	 */
	public static FlushMode interpretExternalSetting(String externalName) {
		if ( externalName == null ) {
			return null;
		}

		try {
			return FlushMode.valueOf( externalName.toUpperCase( Locale.ENGLISH ) );
		}
		catch ( IllegalArgumentException e ) {
			throw new MappingException( "unknown FlushMode : " + externalName );
		}
	}
}
