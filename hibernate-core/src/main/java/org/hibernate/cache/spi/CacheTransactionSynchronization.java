/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

/**
 * Defines a context object that a {@link RegionFactory} is asked to create
 * ({@link RegionFactory#createTransactionContext}}) when a Hibernate Session
 * is created.  It's lifecycle is that of the Session.  It receives
 * "transactional event callbacks" around joining and completing resource
 * transactions.
 *
 * This allows the cache impl to book-keep data related to current transaction,
 * such as and process it in unique ways.  E.g. this allows an impl to perform
 * batch updates if Hibernate is configured to use JDBC-only transactions,
 * and therefore information cannot be retrieved from the JTA transaction
 * assigned to current thread.
 *
 * While transactional semantics might be fully implemented by the cache
 * provider, Hibernate may require different transactional semantics: In order
 * to prevent inconsistent reads, 2LC should not expose entities that are
 * modified in any concurrently executing transactions, and force DB load
 * instead.  Native transactional implementation may provide looser semantics
 * and 2LC implementation has to adapt to these.
 *
 * @implNote Even though a JTA transaction may involve more than one Session
 * the CacheTransactionContext is specific to each Session since the distinction
 * is generally unimportant.  However, a provider is free to attempt to scope
 * these CacheTransactionContext instances in such a way that they may be
 * associated with more than one Session at a time.  This SPI is designed
 * to not require this of the caching impl, but it certainly allows the
 * provider to do it
 *
 * @author Steve Ebersole
 * @author Radim Vansa
 */
public interface CacheTransactionSynchronization {
	/**
	 * What is the start time of this context object?
	 *
	 * @apiNote If not currently joined to a transaction, the timestamp from
	 * the last transaction is safe to use.  If not ever/yet joined to a
	 * transaction, a timestamp at the time the Session/CacheTransactionSynchronization
	 * were created should be returned.
	 *
	 * @implSpec This "timestamp" need not be related to timestamp in the Java
	 * Date/millisecond sense.  It just needs to be an incrementing value.
	 */
	long getCurrentTransactionStartTimestamp();

	/**
	 * Callback that owning Session has become joined to a resource transaction.
	 *
	 * @apiNote Implementors can consider this the effective start of a
	 * transaction.
	 */
	void transactionJoined();

	/**
	 * Callback that the underling resource transaction to which the owning
	 * Session was joined is in the beginning stages of completing.  Note that
	 * this is only called for successful "begin completion" of the underlying
	 * resource transaction (not rolling-back, marked-for-rollback, etc)
	 */
	void transactionCompleting();

	/**
	 * Callback that the underling resource transaction to which the owning
	 * Session was joined is in the "completed" stage.  This method is called
	 * regardless of success or failure of the transaction - the outcome is
	 * passed as a boolean.
	 *
	 * @param successful Was the resource transaction successful?
	 */
	void transactionCompleted(boolean successful);

	/**
	 * Currently not used.  Here for future expansion
	 *
	 * @implNote Currently not used.  JTA defines no standard means to
	 * be notified when a transaction is suspended nor resumed.  Such
	 * a feature is proposed.
	 */
	@SuppressWarnings("unused")
	default void transactionSuspended() {
		// nothing to do since it is currently not used/supported
	}

	/**
	 * Currently not used.  Here for future expansion
	 *
	 * @implNote Currently not used.  JTA defines no standard means to
	 * be notified when a transaction is suspended nor resumed
	 */
	@SuppressWarnings("unused")
	default void transactionResumed() {
		// nothing to do since it is currently not used/supported
	}
}
