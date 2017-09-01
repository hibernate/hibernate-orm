/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

/**
 * Defines a context object that a {@link RegionFactory} is asked to create
 * ({@link RegionFactory#startingTransaction}}) when Hibernate Session becomes
 * part of a "transactional context".
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
 * @author Radim Vansa
 * @author Steve Ebersole
 */
public interface CacheTransactionContext {
	/**
	 * What is the start time of this context object?
	 */
	long getCurrentTransactionStartTimestamp();
}
