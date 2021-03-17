/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.spi;

/**
 * Observer of internal transaction events.
 *
 * @author Steve Ebersole
 */
public interface TransactionObserver {
	/**
	 * Callback for processing the beginning of a transaction.
	 * <p/>
	 * Do not rely on this being called as the transaction may be started in a manner other than through the
	 * {@link org.hibernate.Transaction} API.
	 */
	public void afterBegin();

	/**
	 * Callback for processing the initial phase of transaction completion.
	 */
	public void beforeCompletion();

	/**
	 * Callback for processing the last phase of transaction completion.
	 *
	 * @param successful Was the transaction successful?
	 */
	public void afterCompletion(boolean successful, boolean delayed);
}
