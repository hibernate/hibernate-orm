/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Instances represent a lock mode for a row of a relational
 * database table. It is not intended that users spend much
 * time worrying about locking since Hibernate usually
 * obtains exactly the right lock level automatically.
 * Some "advanced" users may wish to explicitly specify lock
 * levels.
 *
 * @author Gavin King
 *
 * @see Session#lock(Object, LockMode)
 */
public enum LockMode {
	/**
	 * No lock required. If an object is requested with this lock
	 * mode, a <tt>READ</tt> lock will be obtained if it is
	 * necessary to actually read the state from the database,
	 * rather than pull it from a cache.<br>
	 * <br>
	 * This is the "default" lock mode.
	 */
	NONE( 0, "none" ),
	/**
	 * A shared lock. Objects in this lock mode were read from
	 * the database in the current transaction, rather than being
	 * pulled from a cache.
	 */
	READ( 5, "read" ),
	/**
	 * An upgrade lock. Objects loaded in this lock mode are
	 * materialized using an SQL <tt>select ... for update</tt>.
	 *
	 * @deprecated instead use PESSIMISTIC_WRITE
	 */
	@Deprecated
	UPGRADE( 10, "upgrade" ),
	/**
	 * Attempt to obtain an upgrade lock, using an Oracle-style
	 * <tt>select for update nowait</tt>. The semantics of
	 * this lock mode, once obtained, are the same as
	 * <tt>UPGRADE</tt>.
	 */
	UPGRADE_NOWAIT( 10, "upgrade-nowait" ),

	/**
	 * Attempt to obtain an upgrade lock, using an Oracle-style
	 * <tt>select for update skip locked</tt>. The semantics of
	 * this lock mode, once obtained, are the same as
	 * <tt>UPGRADE</tt>.
	 */
	UPGRADE_SKIPLOCKED( 10, "upgrade-skiplocked" ),

	/**
	 * A <tt>WRITE</tt> lock is obtained when an object is updated
	 * or inserted.   This lock mode is for internal use only and is
	 * not a valid mode for <tt>load()</tt> or <tt>lock()</tt> (both
	 * of which throw exceptions if WRITE is specified).
	 */
	WRITE( 10, "write" ),

	/**
	 * Similar to {@link #UPGRADE} except that, for versioned entities,
	 * it results in a forced version increment.
	 *
	 * @deprecated instead use PESSIMISTIC_FORCE_INCREMENT
	 */
	@Deprecated
	FORCE( 15, "force" ),

	/**
	 *  start of javax.persistence.LockModeType equivalent modes
	 */

	/**
	 * Optimistically assume that transaction will not experience contention for
	 * entities.  The entity version will be verified near the transaction end.
	 */
	OPTIMISTIC( 6, "optimistic" ),

	/**
	 * Optimistically assume that transaction will not experience contention for
	 * entities.  The entity version will be verified and incremented near the transaction end.
	 */
	OPTIMISTIC_FORCE_INCREMENT( 7, "optimistic_force_increment" ),

	/**
	 * Implemented as PESSIMISTIC_WRITE.
	 * TODO:  introduce separate support for PESSIMISTIC_READ
	 */
	PESSIMISTIC_READ( 12, "pessimistic_read" ),

	/**
	 * Transaction will obtain a database lock immediately.
	 * TODO:  add PESSIMISTIC_WRITE_NOWAIT
	 */
	PESSIMISTIC_WRITE( 13, "pessimistic_write" ),

	/**
	 * Transaction will immediately increment the entity version.
	 */
	PESSIMISTIC_FORCE_INCREMENT( 17, "pessimistic_force_increment" );

	private final int level;
	private final String externalForm;

	private LockMode(int level, String externalForm) {
		this.level = level;
		this.externalForm = externalForm;
	}

	/**
	 * Check if this lock mode is more restrictive than the given lock mode.
	 *
	 * @param mode LockMode to check
	 *
	 * @return true if this lock mode is more restrictive than given lock mode
	 */
	public boolean greaterThan(LockMode mode) {
		return level > mode.level;
	}

	/**
	 * Check if this lock mode is less restrictive than the given lock mode.
	 *
	 * @param mode LockMode to check
	 *
	 * @return true if this lock mode is less restrictive than given lock mode
	 */
	public boolean lessThan(LockMode mode) {
		return level < mode.level;
	}

	public String toExternalForm() {
		return externalForm;
	}

	public static LockMode fromExternalForm(String externalForm) {
		if ( externalForm == null ) {
			return NONE;
		}

		for ( LockMode lockMode : LockMode.values() ) {
			if ( lockMode.externalForm.equalsIgnoreCase( externalForm ) ) {
				return lockMode;
			}
		}

		throw new IllegalArgumentException( "Unable to interpret LockMode reference from incoming external form : " + externalForm );
	}
}
