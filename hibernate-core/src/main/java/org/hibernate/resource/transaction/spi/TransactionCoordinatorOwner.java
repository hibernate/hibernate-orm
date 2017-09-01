/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.spi;

import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;

/**
 * Models an owner of a TransactionCoordinator.  Mainly used in 2 ways:<ul>
 *     <li>
 *         First to allow the coordinator to determine if its owner is still active (open, etc).
 *     </li>
 *     <li>
 *         Second is to allow the coordinator to dispatch before and after completion events to the owner
 *     </li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface TransactionCoordinatorOwner {
	/**
	 * Is the TransactionCoordinator owner considered active?
	 *
	 * @return {@code true} indicates the owner is still active; {@code false} indicates it is not.
	 */
	boolean isActive();



	// todo (6.0) : in order to properly support HHH-1356 we need to be able to report back to the Session whenever we recognize a new txn boundary.
	//		we need this trigger to create a new CacheTransactionContext from RegionFactory
	//
	//		`#afterTransactionBegin` above only happens for explicit calls to the Hibernate Transaction API, however
	//			we need this trigger no matter who started the transaction
	//
	// 		`#startTransactionBoundary` is a preliminary attempt to allow such a hook.
	//
	//		OPTION #2 : another option is to re-purpose `#afterTransactionBegin` to serve the same
	//		intent described for `#startTransactionBoundary`.  whereas option #1 described above
	//		means minimal changes to existing code, this option #2 would require:
	//			1) alter `SessionImpl#afterTransactionBegin` implementation to not call `Interceptor#afterTransactionBegin`.
	//			2) alter `JdbcResourceLocalTransactionCoordinatorImpl#afterBeginCallback` to (somehow) trigger/call
	//				`Interceptor#afterTransactionBegin`
	//			3) hook JTA-based transaction handling into this `#afterTransactionBegin` call, most likely in
	//				`JtaTransactionCoordinatorImpl#joinJtaTransaction`
	//
	// todo (6.0) : a related (^^) concern - how to handle non-transactional calls
	//		calls to Session outside any transaction.  possible options:
	//			1) we could instead create the CacheTransactionContext when:
	//				a) the Session starts, and
	//				b) whenever a transaction ends
	//				this option has a huge pro in that it means absolutely no code changes are
	//				needed in regards to this "start of a transaction boundary" use case.
	//			2) implicitly create a CacheTransactionContext whenever we need to talk to the
	//				cache provider and there is not a CacheTransactionContext associated with the
	//				Session
	//
	//		overall I personally prefer the approach of:
	// 			1) Add declaration and impl of the `#startTransactionBoundary` method and hook in calls -
	//				a) `JdbcResourceLocalTransactionCoordinatorImpl#afterBeginCallback`
	//				b) `JtaTransactionCoordinatorImpl#joinJtaTransaction`
	//			2) implicitly create a CacheTransactionContext whenever we need to talk to the
	//				cache provider and there is not a CacheTransactionContext associated with the
	//				Session

	/**
	 * Callback indicating recognition of entering into a transactional
	 * context whether that is explicitly via the Hibernate
	 * {@link org.hibernate.Transaction} API or via registration
	 * of Hibernate's JTA Synchronization impl with a JTA Transaction
	 */
	default void startTransactionBoundary() {
		getJdbcSessionOwner().startTransactionBoundary();
	}

	/**
	 * An after-begin callback from the coordinator to its owner.  This should only
	 * be called from handling an explicit start of transaction via the Hibernate
	 * {@link org.hibernate.Transaction} API.
	 */
	void afterTransactionBegin();

	/**
	 * A beforeQuery-completion callback from the coordinator to its owner.
	 */
	void beforeTransactionCompletion();

	/**
	 * An afterQuery-completion callback from the coordinator to its owner.
	 *
	 * @param successful Was the transaction successful?
	 * @param delayed Is this a delayed afterQuery transaction completion call (aka afterQuery a timeout)?
	 */
	void afterTransactionCompletion(boolean successful, boolean delayed);

	JdbcSessionOwner getJdbcSessionOwner();

	/**
	 * Set the effective transaction timeout period for the current transaction, in seconds.
	 *
	 * @param seconds The number of seconds beforeQuery a time out should occur.
	 */
	void setTransactionTimeOut(int seconds);

	void flushBeforeTransactionCompletion();
}
