/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import jakarta.persistence.FindOption;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;

import jakarta.persistence.LockModeType;

/**
 * Instances represent a lock mode for a row of a relational
 * database table. It is not intended that users spend much time
 * worrying about locking since Hibernate usually obtains exactly
 * the right lock level automatically. Some "advanced" users may
 * wish to explicitly specify lock levels.
 * <p>
 * This enumeration of lock modes competes with the JPA-defined
 * {@link LockModeType}, but offers additional options, including
 * {@link #UPGRADE_NOWAIT} and {@link #UPGRADE_SKIPLOCKED}.
 *
 * @author Gavin King
 *
 * @see Session#lock(Object, LockMode)
 * @see LockModeType
 * @see LockOptions
 * @see org.hibernate.annotations.OptimisticLocking
 */
public enum LockMode implements FindOption {
	/**
	 * No lock required. If an object is requested with this lock
	 * mode, a {@link #READ} lock will be obtained if it turns out
	 * to be necessary to actually read the state from the database,
	 * rather than pull it from a cache.
	 * <p>
	 * This is the "default" lock mode, the mode requested by calling
	 * {@link Session#get(Class, Object)} without passing an explicit
	 * mode. It permits the state of an object to be retrieved from
	 * the cache without the cost of database access.
	 *
	 * @see LockModeType#NONE
	 */
	NONE( 0, "none" ),

	/**
	 * A shared lock. Objects in this lock mode were read from the
	 * database in the current transaction, rather than being pulled
	 * from a cache.
	 * <p>
	 * Note that this lock mode is the same as the JPA-defined modes
	 * {@link LockModeType#READ} and {@link LockModeType#OPTIMISTIC}.
	 *
	 * @see LockModeType#OPTIMISTIC
	 */
	READ( 5, "read" ),

	/**
	 * A shared optimistic lock. Assumes that the current transaction
	 * will not experience contention for the state of an entity. The
	 * version will be checked near the end of the transaction, to
	 * verify that this was indeed the case.
	 * <p>
	 * Note that, despite the similar names this lock mode is not the
	 * same as the JPA-defined mode {@link LockModeType#OPTIMISTIC}.
	 */
	OPTIMISTIC( 6, "optimistic" ),

	/**
	 * A kind of exclusive optimistic lock. Assumes that the current
	 * transaction will not experience contention for the state of an
	 * entity. The version will be checked and incremented near the
	 * end of the transaction, to verify that this was indeed the
	 * case, and to signal to concurrent optimistic readers that their
	 * optimistic locks have failed.
	 *
	 * @see LockModeType#OPTIMISTIC_FORCE_INCREMENT
	 */
	OPTIMISTIC_FORCE_INCREMENT( 7, "optimistic_force_increment" ),

	/**
	 * An exclusive write lock. Objects in this lock mode were updated
	 * or inserted in the database in the current transaction.
	 * <p>
	 * This lock mode is for internal use only and is not a legal
	 * argument to {@link Session#get(Class, Object, LockMode)},
	 * {@link Session#refresh(Object, LockMode)}, or
	 * {@link Session#lock(Object, LockMode)}. These methods throw
	 * an exception if {@code WRITE} is given as an argument.
	 * <p>
	 * Note that, despite the similar names, this lock mode is not
	 * the same as the JPA-defined mode {@link LockModeType#WRITE}.
	 */
	@Internal
	WRITE( 10, "write" ),

	/**
	 * A pessimistic upgrade lock, obtained using an Oracle-style
	 * {@code select for update nowait}. The semantics of this
	 * lock mode, if the lock is successfully obtained, are the same
	 * as {@link #PESSIMISTIC_WRITE}. If the lock is not immediately
	 * available, an exception occurs.
	 */
	UPGRADE_NOWAIT( 10, "upgrade-nowait" ),

	/**
	 * A pessimistic upgrade lock, obtained using an Oracle-style
	 * {@code select for update skip locked}. The semantics of this
	 * lock mode, if the lock is successfully obtained, are the same
	 * as {@link #PESSIMISTIC_WRITE}. But if the lock is not
	 * immediately available, no exception occurs, but the locked
	 * row is not returned from the database.
	 */
	UPGRADE_SKIPLOCKED( 10, "upgrade-skiplocked" ),

	/**
	 * A pessimistic shared lock, which prevents concurrent
	 * transactions from writing the locked object. Obtained via
	 * a {@code select for share} statement in dialects where this
	 * syntax is supported, and via {@code select for update} in
	 * other dialects.
	 * <p>
	 * On databases which do not support {@code for share}, this
	 * lock mode is equivalent to {@link #PESSIMISTIC_WRITE}.
	 *
	 * @see LockModeType#PESSIMISTIC_READ
	 */
	PESSIMISTIC_READ( 12, "pessimistic_read" ),

	/**
	 * A pessimistic upgrade lock, which prevents concurrent
	 * transactions from reading or writing the locked object.
	 * Obtained via a {@code select for update} statement.
	 *
	 * @see LockModeType#PESSIMISTIC_WRITE
	 */
	PESSIMISTIC_WRITE( 13, "pessimistic_write" ),

	/**
	 * A pessimistic write lock which immediately increments
	 * the version of the locked object. Obtained by immediate
	 * execution of an {@code update} statement.
	 *
	 * @see LockModeType#PESSIMISTIC_FORCE_INCREMENT
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

	/**
	 * @return an instance of {@link LockOptions} with this lock mode, and
	 *         all other settings defaulted.
	 */
	public LockOptions toLockOptions() {
		// we have to do this in a big switch to
		// avoid circularities in the constructor
		switch (this) {
			case NONE:
				return LockOptions.NONE;
			case READ:
				return LockOptions.READ;
			case OPTIMISTIC:
				return LockOptions.OPTIMISTIC;
			case OPTIMISTIC_FORCE_INCREMENT:
				return LockOptions.OPTIMISTIC_FORCE_INCREMENT;
			case UPGRADE_NOWAIT:
				return LockOptions.UPGRADE_NOWAIT;
			case UPGRADE_SKIPLOCKED:
				return LockOptions.UPGRADE_SKIPLOCKED;
			case PESSIMISTIC_READ:
				return LockOptions.PESSIMISTIC_READ;
			case PESSIMISTIC_WRITE:
				return LockOptions.PESSIMISTIC_WRITE;
			case PESSIMISTIC_FORCE_INCREMENT:
				return LockOptions.PESSIMISTIC_FORCE_INCREMENT;
			case WRITE:
				throw new UnsupportedOperationException("WRITE is not a valid LockMode as an argument");
			default:
				throw new IllegalStateException( "Unexpected value: " + this );
		}
	}
}
