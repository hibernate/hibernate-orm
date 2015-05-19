/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
			return FlushMode.valueOf( externalName.toUpperCase(Locale.ROOT) );
		}
		catch ( IllegalArgumentException e ) {
			throw new MappingException( "unknown FlushMode : " + externalName );
		}
	}
}
