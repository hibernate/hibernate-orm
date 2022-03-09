/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import org.hibernate.jpa.internal.util.LockModeTypeHelper;

import jakarta.persistence.LockModeType;

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
	 * mode, a {@link #READ} lock will be obtained if it is
	 * necessary to actually read the state from the database,
	 * rather than pull it from a cache.
	 * <p>
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
	 * A {@code WRITE} lock is obtained when an object is updated or inserted.
	 *
	 * This lock mode is for internal use only and is not a valid mode for
	 * {@code load()} or {@code lock()}, both of which throw exceptions if
	 * {@code WRITE} is specified.
	 */
	@Internal
	WRITE( 10, "write" ),

	/**
	 * Attempt to obtain an upgrade lock, using an Oracle-style
	 * {@code select for update nowait}. The semantics of
	 * this lock mode, once obtained, are the same as
	 * {@link #PESSIMISTIC_WRITE}.
	 */
	UPGRADE_NOWAIT( 10, "upgrade-nowait" ),

	/**
	 * Attempt to obtain an upgrade lock, using an Oracle-style
	 * {@code select for update skip locked}. The semantics of
	 * this lock mode, once obtained, are the same as
	 * {@link #PESSIMISTIC_WRITE}.
	 */
	UPGRADE_SKIPLOCKED( 10, "upgrade-skiplocked" ),

	/**
	 * Implemented as PESSIMISTIC_WRITE.
	 */
	PESSIMISTIC_READ( 12, "pessimistic_read" ),

	/**
	 * Transaction will obtain a database lock immediately.
	 */
	PESSIMISTIC_WRITE( 13, "pessimistic_write" ),

	/**
	 * Transaction will immediately increment the entity version.
	 */
	PESSIMISTIC_FORCE_INCREMENT( 17, "pessimistic_force_increment" );

	private final int level;
	private final String externalForm;

	LockMode(int level, String externalForm) {
		this.level = level;
		this.externalForm = externalForm;
	}

	public static LockMode fromJpaLockMode(LockModeType lockMode) {
		return LockModeTypeHelper.getLockMode( lockMode );
	}

	public static LockModeType toJpaLockMode(LockMode lockMode) {
		return LockModeTypeHelper.getLockModeType( lockMode );
	}

	public LockModeType toJpaLockMode() {
		return LockModeTypeHelper.getLockModeType( this );
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
			if ( lockMode.externalForm.equals( externalForm ) ) {
				return lockMode;
			}
		}

		if ( externalForm.equalsIgnoreCase( "upgrade" ) ) {
			return PESSIMISTIC_WRITE;
		}

		throw new IllegalArgumentException( "Unable to interpret LockMode reference from incoming external form : " + externalForm );
	}
}
