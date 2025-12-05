/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.emptyIterator;
import static java.util.Collections.emptySet;

/**
 * Contains a set of options describing how a row of a database table
 * mapped by an entity should be locked. For
 * {@link Session#lock(Object, LockOptions)},
 * {@link Session#get(Class, Object, LockOptions)}, or
 * {@link Session#refresh(Object, LockOptions)}, the relevant options
 * are:
 * <ul>
 * <li>the {@linkplain #getLockMode lock mode},
 * <li>the {@linkplain #getTimeOut pessimistic lock timeout}, and
 * <li>the {@linkplain #getLockScope lock scope}, that is, whether
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
 * @deprecated
 * Since JPA 3.2 and Hibernate 7, a {@link LockMode}, {@link Timeout},
 * or {@link PessimisticLockScope} may be passed directly as an option
 * to {@code find()}, {@code refresh()}, or {@code lock()}. Therefore,
 * this class is obsolete as an API and will be moved to an SPI package.
 * <p>
 * For HQL/JPQL queries, locking should be controlled via operations of
 * the {@link org.hibernate.query.SelectionQuery} interface:
 * <ul>
 * <li>A timeout may be set via
 * {@link org.hibernate.query.CommonQueryContract#setTimeout(Timeout)}
 * <li>The {@code PessimisticLockScope} may be set using
 * {@link org.hibernate.query.SelectionQuery#setLockScope(PessimisticLockScope)}
 * <li>Alias-specific lock modes may be specified using
 * {@link org.hibernate.query.SelectionQuery#setLockMode(String, LockMode)}
 * <li>Use of follow-on locking may be enabled via
 * {@link org.hibernate.query.SelectionQuery#setFollowOnLocking(boolean)}
 * </ul>
 * The interface {@link Timeouts} provides several operations to simplify
 * migration.
 *
 * @see Timeout
 * @see Timeouts
 * @see LockMode
 * @see jakarta.persistence.LockModeType
 * @see PessimisticLockScope
 *
 * @author Scott Marlow
 */
@Deprecated(since = "7", forRemoval = true) // moving to an SPI package
public class LockOptions implements Serializable {

	private final boolean immutable;
	private LockMode lockMode;
	private int timeout;
	private Locking.Scope scope;
	private Locking.FollowOn followOnStrategy;

	/**
	 * Construct an instance with mode {@link LockMode#NONE} and
	 * no timeout.
	 *
	 * @see LockMode#NONE
	 * @see Timeouts#WAIT_FOREVER
	 * @see Locking.Scope#ROOT_ONLY
	 * @see Locking.FollowOn#ALLOW
	 */
	public LockOptions() {
		immutable = false;
		lockMode = LockMode.NONE;
		timeout = Timeouts.WAIT_FOREVER_MILLI;
		scope = Locking.Scope.ROOT_ONLY;
		followOnStrategy = Locking.FollowOn.ALLOW;
	}

	/**
	 * Construct an instance with the given {@linkplain LockMode mode}
	 * and no timeout.
	 *
	 * @param lockMode The initial lock mode
	 *
	 * @see Timeouts#WAIT_FOREVER
	 * @see Locking.Scope#ROOT_ONLY
	 * @see Locking.FollowOn#ALLOW
	 */
	public LockOptions(LockMode lockMode) {
		immutable = false;
		this.lockMode = lockMode;
		timeout = Timeouts.WAIT_FOREVER_MILLI;
		this.scope = Locking.Scope.ROOT_ONLY;
		followOnStrategy = Locking.FollowOn.ALLOW;
	}

	/**
	 * Construct an instance with the given {@linkplain LockMode mode}
	 * and timeout.
	 *
	 * @param lockMode The initial lock mode
	 * @param timeout  The initial timeout, in milliseconds
	 *
	 * @see Locking.Scope#ROOT_ONLY
	 * @see Locking.FollowOn#ALLOW
	 */
	public LockOptions(LockMode lockMode, Timeout timeout) {
		immutable = false;
		this.lockMode = lockMode;
		this.timeout = timeout.milliseconds();
		this.scope = Locking.Scope.ROOT_ONLY;
		followOnStrategy = Locking.FollowOn.ALLOW;
	}

	/**
	 * Construct an instance with the given {@linkplain LockMode mode},
	 * timeout, and {@linkplain PessimisticLockScope scope}.
	 *
	 * @param lockMode The initial lock mode
	 * @param timeout The initial timeout
	 * @param jpaScope The initial lock scope
	 *
	 * @see Locking.FollowOn#ALLOW
	 */
	public LockOptions(LockMode lockMode, Timeout timeout, PessimisticLockScope jpaScope) {
		immutable = false;
		this.lockMode = lockMode;
		this.timeout = timeout.milliseconds();
		this.scope = Locking.Scope.fromJpaScope( jpaScope );
		followOnStrategy = Locking.FollowOn.ALLOW;
	}

	/**
	 * Internal operation used to create immutable global instances.
	 *
	 * @see Timeouts#WAIT_FOREVER
	 * @see Locking.Scope#ROOT_ONLY
	 * @see Locking.FollowOn#ALLOW
	 */
	protected LockOptions(boolean immutable, LockMode lockMode) {
		this.immutable = immutable;
		this.lockMode = lockMode;
		timeout = Timeouts.WAIT_FOREVER_MILLI;
		this.scope = Locking.Scope.ROOT_ONLY;
		followOnStrategy = Locking.FollowOn.ALLOW;
	}

	public LockOptions(
			LockMode lockMode,
			int timeout,
			Locking.Scope scope,
			Locking.FollowOn followOnStrategy) {
		this.immutable = false;
		this.lockMode = lockMode;
		this.timeout = timeout;
		this.scope = scope;
		this.followOnStrategy = followOnStrategy;
	}

	/**
	 * Whether this {@code LockOptions} instance is "empty".  Effectively,
	 * this means a {@linkplain #getLockMode() LockMode} of {@linkplain LockMode#NONE NONE}
	 */
	public boolean isEmpty() {
		return lockMode == LockMode.NONE;
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
			throw new UnsupportedOperationException("immutable global instance of LockOptions");
		}
		if ( lockMode == LockMode.UPGRADE_NOWAIT ) {
			timeout = Timeouts.NO_WAIT_MILLI;
		}
		else if ( lockMode == LockMode.UPGRADE_SKIPLOCKED ) {
			timeout = Timeouts.SKIP_LOCKED_MILLI;
		}
		this.lockMode = lockMode;
		return this;
	}

	/**
	 * The timeout associated with {@code this} options, defining a maximum
	 * amount of time that the database should wait to obtain a pessimistic
	 * lock before returning an error to the client.
	 */
	public Timeout getTimeout() {
		return Timeout.milliseconds( getTimeOut() );
	}

	/**
	 * Set the {@linkplain #getTimeout() timeout} associated with {@code this} options.
	 *
	 * @return {@code this} for method chaining
	 *
	 * @see #getTimeout()
	 */
	public LockOptions setTimeout(Timeout timeout) {
		return setTimeOut( timeout.milliseconds() );
	}

	/**
	 * The {@linkplain #getTimeout() timeout}, in milliseconds, associated
	 * with {@code this} options.
	 * <p/>
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
	 * Set the {@linkplain #getTimeout() timeout}, in milliseconds, associated
	 * with {@code this} options.
	 * <p/>
	 * {@link #NO_WAIT}, {@link #WAIT_FOREVER}, or {@link #SKIP_LOCKED}
	 * represent 3 "magic" values.
	 *
	 * @return {@code this} for method chaining
	 *
	 * @see #getTimeOut
	 */
	public LockOptions setTimeOut(int timeout) {
		this.timeout = timeout;
		return this;
	}

	/**
	 * Associated lock scope
	 */
	public Locking.Scope getScope() {
		return scope;
	}

	/**
	 * Set the associated lock scope
	 */
	public LockOptions setScope(Locking.Scope scope) {
		this.scope = scope;
		return this;
	}

	/**
	 * Whether follow-on locking is allowed or, if not, how to handle
	 * cases where Hibernate determines it would need to use follow-on
	 * locking.
	 *
	 * @see org.hibernate.jpa.HibernateHints#HINT_FOLLOW_ON_STRATEGY
	 */
	public Locking.FollowOn getFollowOnStrategy() {
		return followOnStrategy;
	}

	/**
	 * How to handle cases where Hibernate has determined it would need
	 * to use follow-on locking.
	 *
	 * @see #getFollowOnStrategy()
	 */
	public LockOptions setFollowOnStrategy(Locking.FollowOn followOnStrategy) {
		assert followOnStrategy != null;
		this.followOnStrategy = followOnStrategy;
		return this;
	}

	public boolean hasNonDefaultOptions() {
		return timeout != Timeouts.WAIT_FOREVER_MILLI
			|| scope != Locking.Scope.ROOT_ONLY
			|| followOnStrategy != Locking.FollowOn.ALLOW;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// semi-deprecated - these are no longer relevant once we make this a record

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !(object instanceof LockOptions that) ) {
			return false;
		}
		else {
			return timeout == that.timeout
				&& scope == that.scope
				&& lockMode == that.lockMode
				&& Objects.equals( followOnStrategy, that.followOnStrategy );
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash( lockMode, timeout, followOnStrategy, scope );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// deprecations

	/**
	 * Construct an instance with the given {@linkplain LockMode mode}
	 * and timeout.
	 *
	 * @param lockMode The initial lock mode
	 * @param timeout  The initial timeout, in milliseconds
	 *
	 * @deprecated Use {@linkplain #LockOptions(LockMode, Timeout)} instead
	 */
	@Deprecated(since = "7.0")
	public LockOptions(LockMode lockMode, int timeout) {
		this( lockMode, Timeouts.interpretMilliSeconds( timeout ) );
	}


	/**
	 * Construct an instance with the given {@linkplain LockMode mode},
	 * timeout, and {@linkplain PessimisticLockScope scope}.
	 *
	 * @param lockMode The initial lock mode
	 * @param timeout The initial timeout, in milliseconds
	 * @param scope The initial lock scope
	 *
	 * @deprecated Use {@linkplain #LockOptions(LockMode, Timeout, PessimisticLockScope)} instead
	 */
	@Deprecated(since = "7.0")
	public LockOptions(LockMode lockMode, int timeout, PessimisticLockScope scope) {
		this( lockMode, Timeouts.interpretMilliSeconds( timeout ), scope );
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
	 *
	 * @deprecated Use {@linkplain #getScope()} instead
	 */
	@Deprecated(since = "7", forRemoval = true)
	public PessimisticLockScope getLockScope() {
		return scope.getCorrespondingJpaScope();
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
	 * @param jpaScope the new {@link PessimisticLockScope}
	 * @return {@code this} for method chaining
	 *
	 * @deprecated Use {@linkplain #setScope} instead
	 */
	@Deprecated(since = "7", forRemoval = true)
	public LockOptions setLockScope(PessimisticLockScope jpaScope) {
		return setScope( Locking.Scope.fromJpaScope( jpaScope ) );
	}

	/**
	 * Whether follow-on locking is allowed or, if not, how to handle
	 * cases where Hibernate determines it would need to use follow-on
	 * locking.
	 *
	 * @see Locking.FollowOn#asLegacyValue()
	 * @see org.hibernate.jpa.HibernateHints#HINT_FOLLOW_ON_LOCKING
	 * @see org.hibernate.dialect.Dialect#useFollowOnLocking(String, org.hibernate.query.spi.QueryOptions)
	 *
	 * @deprecated Use {@linkplain #getFollowOnStrategy()} instead.
	 */
	@Deprecated(since = "7.1")
	public Boolean getFollowOnLocking() {
		assert followOnStrategy != null;
		return followOnStrategy.asLegacyValue();
	}

	/**
	 * Force enable or disable the use of follow-on locking,
	 * overriding the default behavior of the SQL dialect.
	 *
	 * @param followOnLocking The new follow-on locking setting
	 * @return {@code this} for method chaining
	 *
	 * @see org.hibernate.Locking.FollowOn#fromLegacyValue
	 * @see org.hibernate.jpa.HibernateHints#HINT_FOLLOW_ON_LOCKING
	 * @see org.hibernate.dialect.Dialect#useFollowOnLocking(String, org.hibernate.query.spi.QueryOptions)
	 *
	 * @deprecated Use {@linkplain #setFollowOnStrategy} instead.
	 */
	@Deprecated(since = "7.1")
	public LockOptions setFollowOnLocking(Boolean followOnLocking) {
		followOnStrategy = Locking.FollowOn.fromLegacyValue( followOnLocking );
		return this;
	}

	/**
	 * Set of {@link Map.Entry}s, each associating an alias with its
	 * specified {@linkplain #setAliasSpecificLockMode alias-specific}
	 * {@link LockMode}.
	 *
	 * @return an iterable with the {@link Map.Entry}s
	 *
	 * @deprecated Alias-specific locks are no longer supported, roughly
	 * replaced with {@linkplain #getScope() locking scope}.
	 */
	@Deprecated(since = "7", forRemoval = true)
	public Set<Map.Entry<String,LockMode>> getAliasSpecificLocks() {
		return emptySet();
	}

	/**
	 * Specify the {@link LockMode} to be used for the given query alias.
	 *
	 * @param alias the query alias to which the lock mode applies
	 * @param lockMode the lock mode to apply to the given alias
	 * @return {@code this} for method chaining
	 *
	 * @see org.hibernate.query.Query#setLockMode(String, LockMode)
	 *
	 * @deprecated Alias-specific locks are no longer supported, roughly
	 * replaced with {@linkplain #getScope() locking scope}.
	 */
	@Deprecated(since = "7", forRemoval = true)
	public LockOptions setAliasSpecificLockMode(String alias, LockMode lockMode) {
		if ( immutable ) {
			throw new UnsupportedOperationException("immutable global instance of LockOptions");
		}
		if ( lockMode.greaterThan( this.lockMode ) ) {
			this.lockMode = lockMode;
		}
		return this;
	}

	/**
	 * The number of aliases that have alias-specific lock modes specified.
	 *
	 * @return the number of explicitly defined alias lock modes.
	 *
	 * @deprecated Alias-specific locks are no longer supported, roughly
	 * replaced with {@linkplain #getScope() locking scope}.
	 */
	@Deprecated(since = "7", forRemoval = true)
	public int getAliasLockCount() {
		return 0;
	}

	/**
	 * Whether this {@code LockOptions} instance defines alias-specific lock-modes
	 *
	 * @return {@code true} if this object defines alias-specific lock modes;
	 *        {@code false} otherwise.
	 *
	 * @deprecated Alias-specific locks are no longer supported, roughly
	 * replaced with {@linkplain #getScope() locking scope}.
	 */
	@Deprecated(since = "7", forRemoval = true)
	public boolean hasAliasSpecificLockModes() {
		return false;
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
	 *
	 * @deprecated Alias-specific locks are no longer supported, roughly
	 * replaced with {@linkplain #getScope() locking scope}.
	 */
	@Deprecated(since = "7", forRemoval = true)
	public LockMode getAliasSpecificLockMode(String alias) {
		return lockMode;
	}

	/**
	 * Iterator over {@link Map.Entry}s, each containing an alias and its
	 * {@link LockMode}.
	 *
	 * @return an iterator over the {@link Map.Entry}s
	 *
	 * @deprecated Alias-specific locks are no longer supported, roughly
	 * replaced with {@linkplain #getScope() locking scope}.
	 */
	@Deprecated(since = "7", forRemoval = true)
	public Iterator<Map.Entry<String,LockMode>> getAliasLockIterator() {
		return emptyIterator();
	}

	/**
	 * Determine the {@link LockMode} to apply to the given alias. If no
	 * mode was {@linkplain #setAliasSpecificLockMode(String, LockMode)
	 * explicitly set}, the {@linkplain #getLockMode() overall mode} is
	 * returned. If the overall lock mode is also {@code null},
	 * {@link LockMode#NONE} is returned.
	 * <p>
	 * Differs from {@link #getAliasSpecificLockMode(String)} in that here
	 * we fall back to only returning the overall lock mode.
	 *
	 * @param alias The alias for which to locate the effective lock mode.
	 * @return The effective lock mode.
	 *
	 * @deprecated Alias-specific locks are no longer supported, roughly
	 * replaced with {@linkplain #getScope() locking scope}.
	 *
	 * @see #getLockMode()
	 */
	@Deprecated(since = "7", forRemoval = true)
	public LockMode getEffectiveLockMode(String alias) {
		return lockMode;
	}

	/**
	 * Currently needed for follow-on locking.
	 *
	 * @return The greatest of all requested lock modes.
	 *
	 * @deprecated Alias-specific locks are no longer supported, roughly
	 * replaced with {@linkplain #getScope() locking scope}.
	 *
	 * @see #getLockMode()
	 */
	@Deprecated(since = "7", forRemoval = true)
	public LockMode findGreatestLockMode() {
		return getLockMode();
	}

	/**
	 * Make a copy. The new copy will be mutable even if the original wasn't.
	 *
	 * @return The copy
	 *
	 * @deprecated LockOptions will be made into a record.
	 */
	@Deprecated(since = "7", forRemoval = true)
	public LockOptions makeCopy() {
		final var copy = new LockOptions();
		copy( this, copy );
		return copy;
	}

	/**
	 * Make a copy, unless this is an immutable instance.
	 *
	 * @return The copy, or this if it was immutable.
	 *
	 * @deprecated LockOptions will be made into a record.
	 */
	@Deprecated(since = "7", forRemoval = true)
	public LockOptions makeDefensiveCopy() {
		if ( immutable ) {
			return this;
		}
		else {
			final var copy = new LockOptions();
			copy( this, copy );
			return copy;
		}
	}

	/**
	 * Copy the given lock options into this instance,
	 * merging the alias-specific lock modes.
	 *
	 * @deprecated LockOptions will be made into a record.
	 */
	@Deprecated(since = "7", forRemoval = true)
	public void overlay(LockOptions lockOptions) {
		copy( lockOptions, this );
	}

	/**
	 * Copy the options in the first given instance of
	 * {@code LockOptions} to the second given instance.
	 *
	 * @param source Source for the copy (copied from)
	 * @param destination Destination for the copy (copied to)
	 *
	 * @return destination
	 *
	 * @deprecated LockOptions will be made into a record.
	 */
	@Deprecated(since = "7", forRemoval = true)
	public static LockOptions copy(LockOptions source, LockOptions destination) {
		destination.setLockMode( source.getLockMode() );
		destination.setScope( source.getScope() );
		destination.setTimeOut( source.getTimeOut() );
		destination.setFollowOnStrategy( source.getFollowOnStrategy() );
		return destination;
	}


	/**
	 * Represents {@link LockMode#NONE}, to which timeout and scope are
	 * not applicable.
	 *
	 * @deprecated This, and the other constants on this class, will be removed.
	 */
	@Deprecated(since = "7", forRemoval = true)
	public static final LockOptions NONE = new LockOptions( true, LockMode.NONE );

	/**
	 * Represents {@link LockMode#READ}, to which timeout and scope are
	 * not applicable.
	 *
	 * @deprecated This, and the other constants on this class, will be removed.
	 */
	@Deprecated(since = "7", forRemoval = true)
	public static final LockOptions READ = new LockOptions( true, LockMode.READ );

	/**
	 * Represents {@link LockMode#OPTIMISTIC}.
	 *
	 * @deprecated This, and the other constants on this class, will be removed.
	 */
	@Deprecated(since = "7", forRemoval = true)
	static final LockOptions OPTIMISTIC = new LockOptions( true, LockMode.OPTIMISTIC );

	/**
	 * Represents {@link LockMode#OPTIMISTIC_FORCE_INCREMENT}, to which
	 * timeout and scope are not applicable.
	 *
	 * @deprecated This, and the other constants on this class, will be removed.
	 */
	@Deprecated(since = "7", forRemoval = true)
	static final LockOptions OPTIMISTIC_FORCE_INCREMENT = new LockOptions( true, LockMode.OPTIMISTIC_FORCE_INCREMENT );

	/**
	 * Represents {@link LockMode#PESSIMISTIC_READ}.
	 *
	 * @deprecated This, and the other constants on this class, will be removed.
	 */
	@Deprecated(since = "7", forRemoval = true)
	static final LockOptions PESSIMISTIC_READ = new LockOptions( true, LockMode.PESSIMISTIC_READ );

	/**
	 * Represents {@link LockMode#PESSIMISTIC_WRITE}.
	 *
	 * @deprecated This, and the other constants on this class, will be removed.
	 */
	@Deprecated(since = "7", forRemoval = true)
	static final LockOptions PESSIMISTIC_WRITE = new LockOptions( true, LockMode.PESSIMISTIC_WRITE );

	/**
	 * Represents {@link LockMode#PESSIMISTIC_FORCE_INCREMENT}.
	 *
	 * @deprecated This, and the other constants on this class, will be removed.
	 */
	@Deprecated(since = "7", forRemoval = true)
	static final LockOptions PESSIMISTIC_FORCE_INCREMENT = new LockOptions( true, LockMode.PESSIMISTIC_FORCE_INCREMENT );

	/**
	 * Represents {@link LockMode#UPGRADE_NOWAIT}.
	 *
	 * @deprecated This, and the other constants on this class, will be removed.
	 */
	@Deprecated(since = "7", forRemoval = true)
	static final LockOptions UPGRADE_NOWAIT = new LockOptions( true, LockMode.UPGRADE_NOWAIT );

	/**
	 * Represents {@link LockMode#UPGRADE_SKIPLOCKED}.
	 *
	 * @deprecated This, and the other constants on this class, will be removed.
	 */
	@Deprecated(since = "7", forRemoval = true)
	static final LockOptions UPGRADE_SKIPLOCKED = new LockOptions( true, LockMode.UPGRADE_SKIPLOCKED );

	/**
	 * Represents {@link LockMode#PESSIMISTIC_WRITE} with
	 * {@linkplain #WAIT_FOREVER no timeout}, and
	 * {@linkplain PessimisticLockScope#NORMAL no extension of the
	 * lock to owned collections}.
	 *
	 * @deprecated This, and the other constants on this class, will be removed.
	 */
	@Deprecated(since = "7", forRemoval = true)
	public static final LockOptions UPGRADE = PESSIMISTIC_WRITE;

	/**
	 * @see Timeouts#NO_WAIT_MILLI
	 * @see Timeouts#NO_WAIT
	 * @see #getTimeOut
	 *
	 * @deprecated This, and the other constants on this class, will be removed.
	 */
	@Deprecated(since = "7", forRemoval = true)
	public static final int NO_WAIT = Timeouts.NO_WAIT_MILLI;

	/**
	 * @see Timeouts#WAIT_FOREVER_MILLI
	 * @see Timeouts#WAIT_FOREVER
	 * @see #getTimeOut
	 *
	 * @deprecated This, and the other constants on this class, will be removed.
	 */
	@Deprecated(since = "7", forRemoval = true)
	public static final int WAIT_FOREVER = Timeouts.WAIT_FOREVER_MILLI;

	/**
	 * @see Timeouts#SKIP_LOCKED_MILLI
	 * @see Timeouts#SKIP_LOCKED
	 * @see #getTimeOut()
	 *
	 * @deprecated This, and the other constants on this class, will be removed.
	 */
	@Deprecated(since = "6.2", forRemoval = true)
	public static final int SKIP_LOCKED = -2;
}
