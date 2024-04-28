/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.FindOption;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.RefreshOption;
import org.hibernate.query.Query;
import org.hibernate.query.spi.QueryOptions;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;

/**
 * Contains a set of options describing how a row of a database table
 * mapped by an entity should be locked. For
 * {@link Session#lock(Object, LockOptions)},
 * {@link Session#get(Class, Object, LockOptions)}, or
 * {@link Session#refresh(Object, LockOptions)}, the relevant options
 * are:
 * <ul>
 * <li>the {@linkplain #getLockMode() lock mode},
 * <li>the {@linkplain #getTimeOut() pessimistic lock timeout}, and
 * <li>the {@linkplain #getLockScope() lock scope}, that is, whether
 *     the lock extends to rows of owned collections.
 * </ul>
 * <p>
 * Timeout and lock scope are ignored if the specified {@code LockMode}
 * represents a flavor of {@linkplain LockMode#OPTIMISTIC optimistic}
 * locking.
 * <p>
 * In HQL and criteria queries, lock modes can be defined in an even
 * more granular fashion, with the option to specify a lock mode that
 * {@linkplain #setAliasSpecificLockMode(String, LockMode) applies
 * only to a certain query alias}.
 * <p>
 * Finally, the use of follow-on locking may be force enabled or disabled,
 * overriding the {@linkplain org.hibernate.dialect.Dialect#useFollowOnLocking
 * default behavior of the SQL dialect} by passing a non-null argument
 * to {@link #setFollowOnLocking(Boolean)}.
 *
 * @author Scott Marlow
 */
public class LockOptions implements FindOption, RefreshOption, Serializable {
	/**
	 * Represents {@link LockMode#NONE}, to which timeout and scope are
	 * not applicable.
	 */
	public static final LockOptions NONE = new LockOptions( true, LockMode.NONE );

	/**
	 * Represents {@link LockMode#READ}, to which timeout and scope are
	 * not applicable.
	 */
	public static final LockOptions READ = new LockOptions( true, LockMode.READ );

	/**
	 * Represents {@link LockMode#OPTIMISTIC}.
	 */
	static final LockOptions OPTIMISTIC = new LockOptions( true, LockMode.OPTIMISTIC );

	/**
	 * Represents {@link LockMode#OPTIMISTIC_FORCE_INCREMENT}, to which
	 * timeout and scope are not applicable.
	 */
	static final LockOptions OPTIMISTIC_FORCE_INCREMENT = new LockOptions( true, LockMode.OPTIMISTIC_FORCE_INCREMENT );

	/**
	 * Represents {@link LockMode#PESSIMISTIC_READ}.
	 */
	static final LockOptions PESSIMISTIC_READ = new LockOptions( true, LockMode.PESSIMISTIC_READ );

	/**
	 * Represents {@link LockMode#PESSIMISTIC_WRITE}.
	 */
	static final LockOptions PESSIMISTIC_WRITE = new LockOptions( true, LockMode.PESSIMISTIC_WRITE );

	/**
	 * Represents {@link LockMode#PESSIMISTIC_FORCE_INCREMENT}.
	 */
	static final LockOptions PESSIMISTIC_FORCE_INCREMENT = new LockOptions( true, LockMode.PESSIMISTIC_FORCE_INCREMENT );

	/**
	 * Represents {@link LockMode#UPGRADE_NOWAIT}.
	 */
	static final LockOptions UPGRADE_NOWAIT = new LockOptions( true, LockMode.UPGRADE_NOWAIT );

	/**
	 * Represents {@link LockMode#UPGRADE_SKIPLOCKED}.
	 */
	static final LockOptions UPGRADE_SKIPLOCKED = new LockOptions( true, LockMode.UPGRADE_SKIPLOCKED );

	/**
	 * Represents {@link LockMode#PESSIMISTIC_WRITE} with
	 * {@linkplain #WAIT_FOREVER no timeout}, and
	 * {@linkplain PessimisticLockScope#NORMAL no extension of the
	 * lock to owned collections}.
	 */
	public static final LockOptions UPGRADE = PESSIMISTIC_WRITE;

	/**
	 * Indicates that the database should not wait at all to acquire
	 * a pessimistic lock which is not immediately available. This
	 * has the same effect as {@link LockMode#UPGRADE_NOWAIT}.
	 *
	 * @see #getTimeOut
	 */
	public static final int NO_WAIT = 0;

	/**
	 * Indicates that there is no timeout for the lock acquisition,
	 * that is, that the database should in principle wait forever
	 * to obtain the lock.
	 *
	 * @see #getTimeOut
	 */
	public static final int WAIT_FOREVER = -1;

	/**
	 * Indicates that rows which are already locked should be skipped.
	 *
	 * @see #getTimeOut()
	 * @deprecated use {@link LockMode#UPGRADE_SKIPLOCKED}
	 */
	@Deprecated
	public static final int SKIP_LOCKED = -2;

	private final boolean immutable;
	private LockMode lockMode;
	private int timeout;
	private boolean scope;
	private Boolean followOnLocking;
	private Map<String, LockMode> aliasSpecificLockModes;

	/**
	 * Construct an instance with mode {@link LockMode#NONE} and
	 * timeout {@link #WAIT_FOREVER}.
	 */
	public LockOptions() {
		immutable = false;
		lockMode = LockMode.NONE;
		timeout = WAIT_FOREVER;
	}

	/**
	 * Construct an instance with the given {@linkplain LockMode mode}
	 * and {@link #WAIT_FOREVER}.
	 *
	 * @param lockMode The initial lock mode
	 */
	public LockOptions(LockMode lockMode) {
		immutable = false;
		this.lockMode = lockMode;
		timeout = WAIT_FOREVER;
	}

	/**
	 * Construct an instance with the given {@linkplain LockMode mode}
	 * and timeout.
	 *
	 * @param lockMode The initial lock mode
	 * @param timeout  The initial timeout
	 */
	public LockOptions(LockMode lockMode, int timeout) {
		immutable = false;
		this.lockMode = lockMode;
		this.timeout = timeout;
	}

	/**
	 * Construct an instance with the given {@linkplain LockMode mode},
	 * timeout, and {@link PessimisticLockScope scope}.
	 *
	 * @param lockMode The initial lock mode
	 * @param timeout The initial timeout
	 * @param scope The initial lock scope
	 */
	public LockOptions(LockMode lockMode, int timeout, PessimisticLockScope scope) {
		immutable = false;
		this.lockMode = lockMode;
		this.timeout = timeout;
		this.scope = scope == PessimisticLockScope.EXTENDED;
	}

	/**
	 * Internal operation used to create immutable global instances.
	 */
	private LockOptions(boolean immutable, LockMode lockMode) {
		this.immutable = immutable;
		this.lockMode = lockMode;
		timeout = WAIT_FOREVER;
	}
	/**
	 * Determine of the lock options are empty.
	 *
	 * @return {@code true} if the lock options are equivalent to
	 *         {@link LockOptions#NONE}.
	 */
	public boolean isEmpty() {
		return lockMode == LockMode.NONE
			&& timeout == WAIT_FOREVER
			&& followOnLocking == null
			&& !scope
			&& !hasAliasSpecificLockModes();
	}

	/**
	 * Retrieve the overall lock mode in effect for this set of options.
	 *
	 * @return the overall lock mode
	 */
	public LockMode getLockMode() {
		return lockMode;
	}

	/**
	 * Set the overall {@linkplain LockMode lock mode}. The default is
	 * {@link LockMode#NONE}, that is, no locking at all.
	 *
	 * @param lockMode the new overall lock mode
	 * @return {@code this} for method chaining
	 */
	public LockOptions setLockMode(LockMode lockMode) {
		if ( immutable ) {
			throw new UnsupportedOperationException("immutable global instance of LockMode");
		}
		this.lockMode = lockMode;
		return this;
	}

	/**
	 * Specify the {@link LockMode} to be used for the given query alias.
	 *
	 * @param alias the query alias to which the lock mode applies
	 * @param lockMode the lock mode to apply to the given alias
	 * @return {@code this} for method chaining
	 *
	 * @see Query#setLockMode(String, LockMode)
	 */
	public LockOptions setAliasSpecificLockMode(String alias, LockMode lockMode) {
		if ( immutable ) {
			throw new UnsupportedOperationException("immutable global instance of LockMode");
		}
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
	 * Get the {@link LockMode} explicitly specified for the given alias
	 * via {@link #setAliasSpecificLockMode(String, LockMode)}.
	 * <p>
	 * Differs from {@link #getEffectiveLockMode(String)} in that here we
	 * only return an explicitly specified alias-specific lock mode.
	 *
	 * @param alias The alias for which to locate the explicit lock mode.
	 * @return The explicit lock mode for that alias.
	 */
	public LockMode getAliasSpecificLockMode(String alias) {
		return aliasSpecificLockModes == null ? null : aliasSpecificLockModes.get( alias );
	}

	/**
	 * Determine the {@link LockMode} to apply to the given alias. If no
	 * mode was {@linkplain #setAliasSpecificLockMode(String, LockMode)}
	 * explicitly set}, the {@linkplain #getLockMode()}  overall mode} is
	 * returned. If the overall lock mode is also {@code null},
	 * {@link LockMode#NONE} is returned.
	 * <p>
	 * Differs from {@link #getAliasSpecificLockMode(String)} in that here
	 * we fall back to only returning the overall lock mode.
	 *
	 * @param alias The alias for which to locate the effective lock mode.
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
	 * Does this {@code LockOptions} instance define alias-specific lock
	 * modes?
	 *
	 * @return {@code true} if this object defines alias-specific lock modes;
	 *        {@code false} otherwise.
	 */
	public boolean hasAliasSpecificLockModes() {
		return aliasSpecificLockModes != null && !aliasSpecificLockModes.isEmpty();
	}

	/**
	 * The number of aliases that have alias-specific lock modes specified.
	 *
	 * @return the number of explicitly defined alias lock modes.
	 */
	public int getAliasLockCount() {
		return aliasSpecificLockModes == null ? 0 : aliasSpecificLockModes.size();
	}

	/**
	 * Iterator over {@link Map.Entry}s, each containing an alias and its
	 * {@link LockMode}.
	 *
	 * @return an iterator over the {@link Map.Entry}s
	 * @deprecated use {@link #getAliasSpecificLocks()}
	 */
	@Deprecated
	public Iterator<Map.Entry<String,LockMode>> getAliasLockIterator() {
		return getAliasSpecificLocks().iterator();
	}

	/**
	 * Set of {@link Map.Entry}s, each associating an alias with its
	 * specified {@linkplain #setAliasSpecificLockMode alias-specific}
	 * {@link LockMode}.
	 *
	 * @return an iterable with the {@link Map.Entry}s
	 */
	public Set<Map.Entry<String,LockMode>> getAliasSpecificLocks() {
		return aliasSpecificLockModes == null ? emptySet() : unmodifiableSet( aliasSpecificLockModes.entrySet() );
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
	 * The current timeout, a maximum amount of time in milliseconds
	 * that the database should wait to obtain a pessimistic lock before
	 * returning an error to the client.
	 * <p>
	 * {@link #NO_WAIT}, {@link #WAIT_FOREVER}, or {@link #SKIP_LOCKED}
	 * represent 3 "magic" values.
	 *
	 * @return a timeout in milliseconds, {@link #NO_WAIT},
	 *         {@link #WAIT_FOREVER}, or {@link #SKIP_LOCKED}
	 */
	public int getTimeOut() {
		return timeout;
	}

	/**
	 * Set the timeout, that is, the maximum amount of time in milliseconds
	 * that the database should wait to obtain a pessimistic lock before
	 * returning an error to the client.
	 * <p>
	 * {@link #NO_WAIT}, {@link #WAIT_FOREVER}, or {@link #SKIP_LOCKED}
	 * represent 3 "magic" values.
	 *
	 * @param timeout the new timeout setting, in milliseconds
	 * @return {@code this} for method chaining
	 *
	 * @see #getTimeOut
	 */
	public LockOptions setTimeOut(int timeout) {
		if ( immutable ) {
			throw new UnsupportedOperationException("immutable global instance of LockMode");
		}
		this.timeout = timeout;
		return this;
	}

	/**
	 * The current lock scope:
	 * <ul>
	 * <li>{@link PessimisticLockScope#EXTENDED} means the lock
	 *     extends to rows of owned collections, but
	 * <li>{@link PessimisticLockScope#NORMAL} means only the entity
	 *     table and secondary tables are locked.
	 * </ul>
	 *
	 * @return the current {@link PessimisticLockScope}
	 */
	public PessimisticLockScope getLockScope() {
		return scope ? PessimisticLockScope.EXTENDED : PessimisticLockScope.NORMAL;
	}

	/**
	 * Set the lock scope:
	 * <ul>
	 * <li>{@link PessimisticLockScope#EXTENDED} means the lock
	 *     extends to rows of owned collections, but
	 * <li>{@link PessimisticLockScope#NORMAL} means only the entity
	 *     table and secondary tables are locked.
	 * </ul>
	 *
	 * @param scope the new {@link PessimisticLockScope}
	 * @return {@code this} for method chaining
	 */
	public LockOptions setLockScope(PessimisticLockScope scope) {
		if ( immutable ) {
			throw new UnsupportedOperationException("immutable global instance of LockMode");
		}
		return setScope(scope==PessimisticLockScope.EXTENDED);
	}

	/**
	 * The current lock scope setting:
	 * <ul>
	 * <li>{@code true} means the lock extends to rows of owned
	 *     collections, but
	 * <li>{@code false} means only the entity table and secondary
	 *     tables are locked.
	 * </ul>
	 *
	 * @return {@code true} if the lock extends to owned associations
	 *
	 * @deprecated use {@link #getLockScope()}
	 */
	@Deprecated(since = "6.2")
	public boolean getScope() {
		return scope;
	}

	/**
	 * Set the lock scope setting:
	 * <ul>
	 * <li>{@code true} means the lock extends to rows of owned
	 *     collections, but
	 * <li>{@code false} means only the entity table and secondary
	 *     tables are locked.
	 * </ul>
	 *
	 * @param scope the new scope setting
	 * @return {@code this} for method chaining
	 *
	 * @deprecated use {@link #setLockScope(PessimisticLockScope)}
	 */
	@Deprecated(since = "6.2")
	public LockOptions setScope(boolean scope) {
		if ( immutable ) {
			throw new UnsupportedOperationException("immutable global instance of LockMode");
		}
		this.scope = scope;
		return this;
	}

	/**
	 * Returns a value indicating if follow-on locking was force
	 * enabled or disabled, overriding the default behavior of
	 * the SQL dialect.
	 *
	 * @return {@code true} if follow-on locking was force enabled,
	 *         {@code false} if follow-on locking was force disabled,
	 *         or {@code null} if the default behavior of the dialect
	 *         has not been overridden.
	 *
	 * @see org.hibernate.jpa.HibernateHints#HINT_FOLLOW_ON_LOCKING
	 * @see org.hibernate.dialect.Dialect#useFollowOnLocking(String, QueryOptions)
	 */
	public Boolean getFollowOnLocking() {
		return followOnLocking;
	}

	/**
	 * Force enable or disable the use of follow-on locking,
	 * overriding the default behavior of the SQL dialect.
	 *
	 * @param followOnLocking The new follow-on locking setting
	 * @return {@code this} for method chaining
	 *
	 * @see org.hibernate.jpa.HibernateHints#HINT_FOLLOW_ON_LOCKING
	 * @see org.hibernate.dialect.Dialect#useFollowOnLocking(String, QueryOptions)
	 */
	public LockOptions setFollowOnLocking(Boolean followOnLocking) {
		if ( immutable ) {
			throw new UnsupportedOperationException("immutable global instance of LockMode");
		}
		this.followOnLocking = followOnLocking;
		return this;
	}

	/**
	 * Make a copy. The new copy will be mutable even if the original wasn't.
	 *
	 * @return The copy
	 */
	public LockOptions makeCopy() {
		final LockOptions copy = new LockOptions();
		copy( this, copy );
		return copy;
	}

	/**
	 * Make a copy, unless this is an immutable instance.
	 *
	 * @return The copy, or this if it was immutable.
	 */
	public LockOptions makeDefensiveCopy() {
		if ( immutable ) {
			return this;
		}
		else {
			final LockOptions copy = new LockOptions();
			copy( this, copy );
			return copy;
		}
	}

	/**
	 * Copy the given lock options into this instance,
	 * merging the alias-specific lock modes.
	 */
	public void overlay(LockOptions lockOptions) {
		setLockMode( lockOptions.getLockMode() );
		setScope( lockOptions.getScope() );
		setTimeOut( lockOptions.getTimeOut() );
		if ( lockOptions.aliasSpecificLockModes != null ) {
			lockOptions.aliasSpecificLockModes.forEach(this::setAliasSpecificLockMode);
		}
		setFollowOnLocking( lockOptions.getFollowOnLocking() );
	}

	/**
	 * Copy the options in the first given instance of
	 * {@code LockOptions} to the second given instance.
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
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !(object instanceof LockOptions) ) {
			return false;
		}
		else {
			final LockOptions that = (LockOptions) object;
			return timeout == that.timeout
				&& scope == that.scope
				&& lockMode == that.lockMode
				&& Objects.equals( aliasSpecificLockModes, that.aliasSpecificLockModes )
				&& Objects.equals( followOnLocking, that.followOnLocking );
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash( lockMode, timeout, aliasSpecificLockModes, followOnLocking, scope );
	}
}
