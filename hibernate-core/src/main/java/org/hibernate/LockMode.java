package org.hibernate;

import jakarta.persistence.FindOption;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.RefreshOption;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;

import java.util.Locale;

import static org.hibernate.Timeouts.NO_WAIT_MILLI;
import static org.hibernate.Timeouts.SKIP_LOCKED_MILLI;

/**
 * Describes the concurrency protection requested for an entity, or the
 * lock state recorded by a stateful session. Explicit optimistic modes
 * validate a versioned entity even when it is unchanged. Automatic checks
 * on entity updates and deletes instead follow the mapping's
 * {@linkplain org.hibernate.annotations.OptimisticLocking optimistic
 * locking strategy}, including versionless value comparisons.
 * Merely reading an entity does not schedule explicit optimistic validation.
 * These contracts assume a transaction isolation level of at least
 * {@linkplain java.sql.Connection#TRANSACTION_READ_COMMITTED read committed}.
 * <p>
 * A partial order of lock modes is defined such that every
 * optimistic lock mode is considered a weaker sort of lock than
 * every pessimistic lock mode. If a session already holds a
 * stronger lock level on a given entity when a weaker lock level
 * is requested, then the stronger lock level is retained.
 * A pessimistic request with {@link jakarta.persistence.PessimisticLockScope#EXTENDED}
 * scope may still require acquisition of locks on collection rows, even when
 * its lock level is equal to or weaker than the level already held.
 * Repeating such a request can acquire locks for replaced or changed
 * collections. Scope acquisition alone does not repeat a forced version
 * increment.
 * <p>
 * Note that, in particular, a request for the optimistic lock
 * level {@link #OPTIMISTIC_FORCE_INCREMENT} on an entity for
 * which any pessimistic lock is already held has no effect,
 * and does not force a version increment.
 * <p>
 * Pessimistic modes acquire database protection against conflicting
 * writes to the covered rows until transaction completion. The effect on
 * ordinary readers depends on the database and isolation configuration:
 * snapshot reads may proceed even while an exclusive lock is held.
 * The {@linkplain PessimisticLockScope scope} determines the rows covered;
 * locking an entity does not automatically protect every reachable entity
 * or prevent every phantom insertion.
 * <p>
 * Lock acquisition is not a general refresh of existing managed state.
 * {@link Session#refresh(Object)} reloads state while preserving existing
 * pessimistic protection. The state visible to that reload depends on the
 * database's current-read and snapshot rules; repeatable-read isolation
 * does not universally imply that refresh observes the earlier values.
 * <p>
 * This enumeration complements the Jakarta Persistence-defined
 * {@link LockModeType}, but offers additional options, including
 * {@link #UPGRADE_NOWAIT} and {@link #UPGRADE_SKIPLOCKED}.
 *
 * @author Gavin King
 *
 * @see Session#lock(Object, LockMode)
 * @see Session#lock(Object, LockMode, jakarta.persistence.LockOption...)
 * @see Session#find(Class, Object, FindOption...)
 * @see Session#refresh(Object, RefreshOption...)
 * @see LockModeType
 * @see org.hibernate.annotations.OptimisticLocking
 */
public enum LockMode implements FindOption, RefreshOption {
	/**
	 * No explicit lock requested. A stateful session records {@link #READ}
	 * if it needs to read the entity from the database instead of a cache.
	 * Automatic mutation checks still follow the entity's mapping.
	 * <p>
	 * This is the "default" lock mode, the mode requested by calling
	 * {@link Session#find(Class, Object)} without passing an explicit
	 * mode. It permits the state of an object to be retrieved from
	 * the cache without the cost of database access.
	 *
	 * @see LockModeType#NONE
	 */
	NONE,

	/**
	 * Records that objects in this mode were read from the
	 * database in the current transaction, rather than being pulled
	 * from a cache. This mode does not itself promise a transaction-long
	 * shared database lock or schedule optimistic validation at completion.
	 * <p>
	 * Note that, despite the similar names this lock mode is not the
	 * same as the JPA-defined mode {@link LockModeType#READ}.
	 */
	READ,

	/**
	 * Requests validation of the entity's version even if the entity is
	 * unchanged. A stateful session schedules a version check before
	 * transaction completion while the entity remains managed.
	 * A {@link StatelessSession} instead obtains immediate database
	 * protection retained until transaction completion; the request is
	 * rejected if the resolved concurrency configuration cannot provide it.
	 * <p>
	 * Only legal for versioned entity types.
	 * <p>
	 * Note that this lock mode is the same as the JPA-defined modes
	 * {@link LockModeType#READ} and {@link LockModeType#OPTIMISTIC}.
	 *
	 * @see LockModeType#OPTIMISTIC
	 */
	OPTIMISTIC,

	/**
	 * Requests version validation and an increment even if the entity is
	 * unchanged, signaling a change to concurrent optimistic readers.
	 * A stateful session schedules the increment before transaction
	 * completion while the entity remains managed. A {@link StatelessSession}
	 * performs the increment immediately.
	 * <p>
	 * A request for this mode does not independently force an increment
	 * when a pessimistic mode is already held by a stateful session.
	 * <p>
	 * Only legal for versioned entity types.
	 *
	 * @see LockModeType#OPTIMISTIC_FORCE_INCREMENT
	 */
	OPTIMISTIC_FORCE_INCREMENT,

	/**
	 * An exclusive write lock. Objects in this lock mode were updated
	 * or inserted in the database in the current transaction.
	 * <p>
	 * This lock mode is for internal use only and is not a legal
	 * argument to {@link Session#get(Class, Object, LockMode)},
	 * {@link Session#refresh(Object, RefreshOption...)}, or
	 * {@link Session#lock(Object, LockMode)}. These methods throw
	 * an exception if {@code WRITE} is given as an argument.
	 * <p>
	 * Note that, despite the similar names, this lock mode is not
	 * the same as the JPA-defined mode {@link LockModeType#WRITE}.
	 */
	@Internal
	WRITE,

	/**
	 * Requests shared database protection against conflicting writes to
	 * the covered rows, retained until transaction completion. The dialect
	 * may use a locking clause or table hints.
	 * <p>
	 * A stronger lock may be used when shared protection is unavailable.
	 *
	 * @see LockModeType#PESSIMISTIC_READ
	 */
	PESSIMISTIC_READ,

	/**
	 * Requests exclusive database protection against conflicting writes
	 * and incompatible locking requests, retained until transaction completion.
	 * The dialect may use {@code select for update} or table hints.
	 * Ordinary snapshot readers may still read the covered rows.
	 *
	 * @see LockModeType#PESSIMISTIC_WRITE
	 */
	PESSIMISTIC_WRITE,

	/**
	 * A pessimistic write lock which immediately increments
	 * the version of the locked object. Obtained by immediate
	 * execution of an {@code update} statement.
	 * <p>
	 * Only legal for versioned entity types.
	 * Support also depends on the version generator. Upgrading from
	 * {@link #PESSIMISTIC_READ} or {@link #PESSIMISTIC_WRITE} increments the
	 * version immediately. Repeating this mode when it is already held by a
	 * stateful session does not itself cause another increment.
	 *
	 * @see LockModeType#PESSIMISTIC_FORCE_INCREMENT
	 */
	PESSIMISTIC_FORCE_INCREMENT,

	/**
	 * A pessimistic upgrade lock, obtained using an Oracle-style
	 * {@code select for update nowait}. The semantics of this
	 * lock mode, if the lock is successfully obtained, are the same
	 * as {@link #PESSIMISTIC_WRITE}. If the lock is not immediately
	 * available, an exception occurs.
	 *
	 * @apiNote This lock-mode is intended for use as a JPA
	 * {@linkplain HibernateHints#HINT_NATIVE_LOCK_MODE query hint}.
	 * Other cases should use the combination of
	 * {@linkplain #PESSIMISTIC_WRITE} and {@linkplain Timeouts#NO_WAIT}
	 * as find/refresh options - e.g. {@code session.find(Book.class, 1, PESSIMISTIC_WRITE, NO_WAIT)}
	 *
	 * @see #PESSIMISTIC_WRITE
	 * @see Timeouts#NO_WAIT
	 */
	UPGRADE_NOWAIT,

	/**
	 * A pessimistic upgrade lock, obtained using an Oracle-style
	 * {@code select for update skip locked}. The semantics of this
	 * lock mode, if the lock is successfully obtained, are the same
	 * as {@link #PESSIMISTIC_WRITE}. But if the lock is not
	 * immediately available, no exception occurs, but the locked
	 * row is not returned from the database.
	 * This requires a supported direct-locking selection; follow-on
	 * acquisition and locking an already managed entity do not support it.
	 *
	 * @apiNote This lock-mode is intended for use as a JPA
	 * {@linkplain HibernateHints#HINT_NATIVE_LOCK_MODE query hint}.
	 * Other cases should use the combination of
	 * {@linkplain #PESSIMISTIC_WRITE} and {@linkplain Timeouts#SKIP_LOCKED}
	 * as find/refresh options - e.g. {@code session.find(Book.class, 1, PESSIMISTIC_WRITE, SKIP_LOCKED)}
	 *
	 * @see #PESSIMISTIC_WRITE
	 * @see Timeouts#SKIP_LOCKED
	 */
	UPGRADE_SKIPLOCKED;

	/**
	 * @return an instance with the same semantics as the given JPA
	 *         {@link LockModeType}.
	 */
	public static LockMode fromJpaLockMode(LockModeType lockMode) {
		return LockModeTypeHelper.getLockMode( lockMode );
	}

	/**
	 * @return an instance of the JPA-defined {@link LockModeType}
	 *         with similar semantics to the given {@code LockMode}.
	 */
	public static LockModeType toJpaLockMode(LockMode lockMode) {
		return LockModeTypeHelper.getLockModeType( lockMode );
	}

	/**
	 * @return an instance of the JPA-defined {@link LockModeType}
	 *         with similar semantics to this {@code LockMode}.
	 */
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
		return level() > mode.level();
	}

	/**
	 * Check if this lock mode is less restrictive than the given lock mode.
	 *
	 * @param mode LockMode to check
	 *
	 * @return true if this lock mode is less restrictive than given lock mode
	 */
	public boolean lessThan(LockMode mode) {
		return level() < mode.level();
	}

	/**
	 * Does this lock mode require a {@linkplain jakarta.persistence.Version version}?
	 *
	 * @return {@code true} if this lock mode only applies to versioned entities
	 */
	public boolean requiresVersion() {
		return this == OPTIMISTIC
			|| this == OPTIMISTIC_FORCE_INCREMENT
			|| this == PESSIMISTIC_FORCE_INCREMENT;
	}

	public String toExternalForm() {
		final String externalForm = toString().toLowerCase(Locale.ROOT);
		return this == UPGRADE_NOWAIT || this == UPGRADE_SKIPLOCKED
				? externalForm.replace('_', '-')
				: externalForm;
	}

	/**
	 * Determines a partial order on the lock modes,
	 * based on how "exclusive" the lock is.
	 * <p>
	 * Note that {@link #PESSIMISTIC_READ} is, quite
	 * arbitrarily, treated as more exclusive than
	 * {@link #OPTIMISTIC_FORCE_INCREMENT}. Thus, if
	 * the program holds any pessimistic lock on an
	 * instance, and requests
	 * {@code OPTIMISTIC_FORCE_INCREMENT}, then no
	 * forced version increment will occur.
	 */
	private int level() {
		return switch (this) {
			case NONE -> 0;
			case READ -> 1;
			case OPTIMISTIC -> 2;
			case OPTIMISTIC_FORCE_INCREMENT -> 3;
			case PESSIMISTIC_READ -> 4;
			case UPGRADE_NOWAIT, UPGRADE_SKIPLOCKED, PESSIMISTIC_WRITE -> 5;
			case PESSIMISTIC_FORCE_INCREMENT, WRITE -> 6;
		};
	}

	public static LockMode fromExternalForm(String externalForm) {
		if ( externalForm == null ) {
			return NONE;
		}

		for ( LockMode lockMode : values() ) {
			if ( lockMode.toExternalForm().equalsIgnoreCase( externalForm ) ) {
				return lockMode;
			}
		}

		if ( externalForm.equalsIgnoreCase( "upgrade" ) ) {
			return PESSIMISTIC_WRITE;
		}

		throw new IllegalArgumentException( "Unable to interpret LockMode reference from incoming external form: " + externalForm );
	}

	/**
	 * @return an instance of {@link LockOptions} with this lock mode, and
	 *         all other settings defaulted.
	 *
	 * @deprecated With no replacement; {@linkplain LockOptions} is no longer considered an API.
	 */
	@Deprecated(since = "7", forRemoval = true)
	public LockOptions toLockOptions() {
		return switch (this) {
			case NONE -> new LockOptions();
			case READ -> new LockOptions( READ );
			case OPTIMISTIC -> new LockOptions( OPTIMISTIC );
			case OPTIMISTIC_FORCE_INCREMENT -> new LockOptions( OPTIMISTIC_FORCE_INCREMENT );
			case UPGRADE_NOWAIT -> new LockOptions( PESSIMISTIC_WRITE, NO_WAIT_MILLI, PessimisticLockScope.NORMAL, Locking.FollowOn.ALLOW );
			case UPGRADE_SKIPLOCKED -> new LockOptions( PESSIMISTIC_WRITE, SKIP_LOCKED_MILLI, PessimisticLockScope.NORMAL, Locking.FollowOn.ALLOW );
			case PESSIMISTIC_READ -> new LockOptions( PESSIMISTIC_READ );
			case PESSIMISTIC_WRITE -> new LockOptions( PESSIMISTIC_WRITE );
			case PESSIMISTIC_FORCE_INCREMENT -> new LockOptions( PESSIMISTIC_FORCE_INCREMENT );
			case WRITE -> throw new UnsupportedOperationException( "WRITE is not a valid LockMode as an argument" );
		};
	}

	public boolean isPessimistic() {
		return this == PESSIMISTIC_READ
			|| this == PESSIMISTIC_WRITE
			|| this == PESSIMISTIC_FORCE_INCREMENT
			|| this == UPGRADE_NOWAIT
			|| this == UPGRADE_SKIPLOCKED;
	}
}
