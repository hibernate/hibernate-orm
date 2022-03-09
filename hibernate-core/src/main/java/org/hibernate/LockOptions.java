/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.query.Query;

/**
 * Contains locking details (LockMode, Timeout and Scope).
 * 
 * @author Scott Marlow
 */
public class LockOptions implements Serializable {
	/**
	 * Represents LockMode.NONE (timeout + scope do not apply).
	 */
	public static final LockOptions NONE = new LockOptions(LockMode.NONE);

	/**
	 * Represents LockMode.READ (timeout + scope do not apply).
	 */
	public static final LockOptions READ = new LockOptions(LockMode.READ);

	/**
	 * Represents LockMode.UPGRADE (will wait forever for lock and scope of false meaning only entity is locked).
	 */
	@SuppressWarnings("deprecation")
	public static final LockOptions UPGRADE = new LockOptions(LockMode.PESSIMISTIC_WRITE);

	/**
	 * Indicates that the database should not wait at all to acquire the pessimistic lock.
	 * @see #getTimeOut
	 */
	public static final int NO_WAIT = 0;

	/**
	 * Indicates that there is no timeout for the acquisition.
	 * @see #getTimeOut
	 */
	public static final int WAIT_FOREVER = -1;

	/**
	 * Indicates that rows that are already locked should be skipped.
	 * @see #getTimeOut()
	 */
	public static final int SKIP_LOCKED = -2;

	private LockMode lockMode = LockMode.NONE;
	private int timeout = WAIT_FOREVER;

	private Map<String,LockMode> aliasSpecificLockModes;

	private Boolean followOnLocking;

	/**
	 * Constructs a LockOptions with all default options.
	 */
	public LockOptions() {
	}

	/**
	 * Constructs a LockOptions with the given lock mode.
	 *
	 * @param lockMode The lock mode to use
	 */
	public LockOptions( LockMode lockMode) {
		this.lockMode = lockMode;
	}

	/**
	 * Returns whether the lock options are empty.
	 *
	 * @return If the lock options is equivalent to {@link LockOptions#NONE}.
	 */
	public boolean isEmpty() {
		return lockMode == LockMode.NONE && timeout == WAIT_FOREVER && followOnLocking == null && !scope && !hasAliasSpecificLockModes();
	}

	/**
	 * Retrieve the overall lock mode in effect for this set of options.
	 * <p/>
	 * In certain contexts (hql and criteria), lock-modes can be defined in an
	 * even more granular {@link #setAliasSpecificLockMode(String, LockMode) per-alias} fashion
	 *
	 * @return The overall lock mode.
	 */
	public LockMode getLockMode() {
		return lockMode;
	}

	/**
	 * Set the overall {@link LockMode} to be used.  The default is
	 * {@link LockMode#NONE}
	 *
	 * @param lockMode The new overall lock mode to use.
	 *
	 * @return this (for method chaining).
	 */
	public LockOptions setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
		return this;
	}

	/**
	 * Specify the {@link LockMode} to be used for a specific query alias.
	 *
	 * @param alias used to reference the LockMode.
	 * @param lockMode The lock mode to apply to the given alias
	 * @return this LockRequest instance for operation chaining.
	 *
	 * @see Query#setLockMode(String, LockMode)
	 */
	public LockOptions setAliasSpecificLockMode(String alias, LockMode lockMode) {
		if ( aliasSpecificLockModes == null ) {
			aliasSpecificLockModes = new LinkedHashMap<>();
		}
		if ( lockMode == null ) {
			aliasSpecificLockModes.remove( alias );
		}
		else {
			aliasSpecificLockModes.put( alias, lockMode );
		}
		return this;
	}

	/**
	 * Get the {@link LockMode} explicitly specified for the given alias via
	 * {@link #setAliasSpecificLockMode}
	 * <p/>
	 * Differs from {@link #getEffectiveLockMode} in that here we only return
	 * explicitly specified alias-specific lock modes.
	 *
	 * @param alias The alias for which to locate the explicit lock mode.
	 *
	 * @return The explicit lock mode for that alias.
	 */
	public LockMode getAliasSpecificLockMode(String alias) {
		if ( aliasSpecificLockModes == null ) {
			return null;
		}
		return aliasSpecificLockModes.get( alias );
	}

	/**
	 * Determine the {@link LockMode} to apply to the given alias.  If no
	 * mode was explicitly {@linkplain #setAliasSpecificLockMode set}, the
	 * {@linkplain #getLockMode overall mode} is returned.  If the overall
	 * lock mode is {@code null} as well, {@link LockMode#NONE} is returned.
	 * <p/>
	 * Differs from {@link #getAliasSpecificLockMode} in that here we fallback to we only return
	 * the overall lock mode.
	 *
	 * @param alias The alias for which to locate the effective lock mode.
	 *
	 * @return The effective lock mode.
	 */
	public LockMode getEffectiveLockMode(String alias) {
		LockMode lockMode = getAliasSpecificLockMode( alias );
		if ( lockMode == null ) {
			lockMode = this.lockMode;
		}
		return lockMode == null ? LockMode.NONE : lockMode;
	}

	/**
	 * Does this {@code LockOptions} instance define alias-specific lock modes?
	 *
	 * @return {@code true} if this object defines alias-specific lock modes; {@code false} otherwise.
	 */
	public boolean hasAliasSpecificLockModes() {
		return aliasSpecificLockModes != null
				&& ! aliasSpecificLockModes.isEmpty();
	}

	/**
	 * Get the number of aliases that have specific lock modes defined.
	 *
	 * @return the number of explicitly defined alias lock modes.
	 */
	public int getAliasLockCount() {
		if ( aliasSpecificLockModes == null ) {
			return 0;
		}
		return aliasSpecificLockModes.size();
	}

	/**
	 * Iterator for accessing Alias (key) and LockMode (value) as Map.Entry.
	 *
	 * @return Iterator for accessing the Map.Entry's
	 */
	public Iterator<Map.Entry<String,LockMode>> getAliasLockIterator() {
		return getAliasSpecificLocks().iterator();
	}

	/**
	 * Iterable access to alias (key) and LockMode (value) as Map.Entry.
	 *
	 * @return Iterable for accessing the Map.Entry's
	 */
	public Iterable<Map.Entry<String,LockMode>> getAliasSpecificLocks() {
		if ( aliasSpecificLockModes == null ) {
			return Collections.emptyList();
		}
		return aliasSpecificLockModes.entrySet();
	}

	/**
	 * Currently needed for follow-on locking.
	 *
	 * @return The greatest of all requested lock modes.
	 */
	public LockMode findGreatestLockMode() {
		LockMode lockModeToUse = getLockMode();
		if ( lockModeToUse == null ) {
			lockModeToUse = LockMode.NONE;
		}

		if ( aliasSpecificLockModes == null ) {
			return lockModeToUse;
		}

		for ( LockMode lockMode : aliasSpecificLockModes.values() ) {
			if ( lockMode.greaterThan( lockModeToUse ) ) {
				lockModeToUse = lockMode;
			}
		}

		return lockModeToUse;
	}

	/**
	 * Retrieve the current timeout setting.
	 * <p/>
	 * The timeout is the amount of time, in milliseconds, we should instruct the database
	 * to wait for any requested pessimistic lock acquisition.
	 * <p/>
	 * {@link #NO_WAIT}, {@link #WAIT_FOREVER} or {@link #SKIP_LOCKED} represent 3 "magic" values.
	 *
	 * @return timeout in milliseconds, {@link #NO_WAIT}, {@link #WAIT_FOREVER} or {@link #SKIP_LOCKED}
	 */
	public int getTimeOut() {
		return timeout;
	}

	/**
	 * Set the timeout setting.
	 * <p/>
	 * See {@link #getTimeOut} for a discussion of meaning.
	 *
	 * @param timeout The new timeout setting, in milliseconds
	 *
	 * @return this (for method chaining).
	 *
	 * @see #getTimeOut
	 */
	public LockOptions setTimeOut(int timeout) {
		this.timeout = timeout;
		return this;
	}

	private boolean scope;

	/**
	 * Retrieve the current lock scope setting.
	 * <p/>
	 * "scope" is a JPA defined term.  It is basically a cascading of the lock to associations.
	 *
	 * @return true if locking will be extended to owned associations
	 */
	public boolean getScope() {
		return scope;
	}

	/**
	 * Set the scope.
	 *
	 * @param scope The new scope setting
	 *
	 * @return this (for method chaining).
	 */
	public LockOptions setScope(boolean scope) {
		this.scope = scope;
		return this;
	}

	/**
	 * Retrieve the current follow-on-locking setting.
	 *
	 * @return true if follow-on-locking is enabled
	 */
	public Boolean getFollowOnLocking() {
		return followOnLocking;
	}

	/**
	 * Set the follow-on-locking setting.
	 * @param followOnLocking The new follow-on-locking setting
	 * @return this (for method chaining).
	 */
	public LockOptions setFollowOnLocking(Boolean followOnLocking) {
		this.followOnLocking = followOnLocking;
		return this;
	}

	/**
	 * Make a copy.
	 *
	 * @return The copy
	 */
	public LockOptions makeCopy() {
		final LockOptions copy = new LockOptions();
		copy( this, copy );
		return copy;
	}

	public void overlay(LockOptions lockOptions) {
		copy( lockOptions, this );
	}

	/**
	 * Perform a shallow copy.
	 *
	 * @param source Source for the copy (copied from)
	 * @param destination Destination for the copy (copied to)
	 *
	 * @return destination
	 */
	public static LockOptions copy(LockOptions source, LockOptions destination) {
		destination.setLockMode( source.getLockMode() );
		destination.setScope( source.getScope() );
		destination.setTimeOut( source.getTimeOut() );
		if ( source.aliasSpecificLockModes != null ) {
			destination.aliasSpecificLockModes = new HashMap<>( source.aliasSpecificLockModes );
		}
		destination.setFollowOnLocking( source.getFollowOnLocking() );
		return destination;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		LockOptions that = (LockOptions) o;

		if ( timeout != that.timeout ) {
			return false;
		}
		if ( scope != that.scope ) {
			return false;
		}
		if ( lockMode != that.lockMode ) {
			return false;
		}
		if ( aliasSpecificLockModes != null ?
				!aliasSpecificLockModes.equals( that.aliasSpecificLockModes ) :
				that.aliasSpecificLockModes != null ) {
			return false;
		}
		return followOnLocking != null ? followOnLocking.equals( that.followOnLocking ) : that.followOnLocking == null;
	}

	@Override
	public int hashCode() {
		int result = lockMode != null ? lockMode.hashCode() : 0;
		result = 31 * result + timeout;
		result = 31 * result + ( aliasSpecificLockModes != null ? aliasSpecificLockModes.hashCode() : 0 );
		result = 31 * result + ( followOnLocking != null ? followOnLocking.hashCode() : 0 );
		result = 31 * result + ( scope ? 1 : 0 );
		return result;
	}
}
