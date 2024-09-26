/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.cfg;


import jakarta.persistence.spi.PersistenceUnitInfo;

/**
 * @author Steve Ebersole
 */
public interface TransactionSettings {
	/**
	 * Specify the {@link org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder}
	 * implementation to use for creating instances of
	 * {@link org.hibernate.resource.transaction.spi.TransactionCoordinator} which the interface
	 * Hibernate uses to manage transactions.
	 * <p/>
	 * Accepts either:
	 * <ul>
	 *     <li>an instance of {@code TransactionCoordinatorBuilder},
	 *     <li>a {@link Class} representing a class that implements {@code TransactionCoordinatorBuilder},
	 *     <li>the name of a class that implements {@code TransactionCoordinatorBuilder},
	 *     <li>{@code jta} or {@code jdbc}</li>
	 * </ul>
	 * <p/>
	 *
	 * @settingDefault With Jakarta Persistence bootstrapping, based on the persistence unit's {@link PersistenceUnitInfo#getTransactionType()};
	 * otherwise {@code jdbc}.
	 *
	 * @implSpec With non Jakarta Persistence bootstrapping, Hibernate will use {@code jdbc} as the default which
	 * will cause problems if the application actually uses JTA-based transactions.
	 *
	 * @see #JTA_PLATFORM
	 *
	 * @since 5.0
	 */
	String TRANSACTION_COORDINATOR_STRATEGY = "hibernate.transaction.coordinator_class";

	/**
	 * Specifies the {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform}
	 * implementation to use for integrating with JTA, either:
	 * <ul>
	 *     <li>an instance of {@code JtaPlatform}, or
	 *     <li>the name of a class that implements {@code JtaPlatform}.
	 *     <li>short name of a class (sans package name) that implements {@code JtaPlatform}.
	 * </ul>
	 *
	 * @see #JTA_PLATFORM_RESOLVER
	 *
	 * @since 4.0
	 */
	String JTA_PLATFORM = "hibernate.transaction.jta.platform";

	/**
	 * Specifies a {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformResolver}
	 * implementation that should be used to obtain an instance of
	 * {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform}.
	 *
	 * @since 4.3
	 */
	String JTA_PLATFORM_RESOLVER = "hibernate.transaction.jta.platform_resolver";

	/**
	 * When enabled, specifies that the {@link jakarta.transaction.UserTransaction} should
	 * be used in preference to the {@link jakarta.transaction.TransactionManager} for JTA
	 * transaction management.
	 * <p>
	 * By default, the {@code TransactionManager} is preferred.
	 *
	 * @see org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform#retrieveUserTransaction
	 * @see org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform#retrieveTransactionManager
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyPreferUserTransactions(boolean)
	 *
	 * @settingDefault {@code false} as {@code TransactionManager} is preferred.
	 *
	 * @since 5.0
	 */
	String PREFER_USER_TRANSACTION = "hibernate.jta.prefer_user_transaction";

	/**
	 * When enabled, indicates that it is safe to cache {@link jakarta.transaction.TransactionManager}
	 * references in the {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform}
	 *
	 * @settingDefault Generally {@code true}, though {@code JtaPlatform} implementations
	 * can do their own thing.
	 *
	 * @since 4.0
	 */
	String JTA_CACHE_TM = "hibernate.jta.cacheTransactionManager";

	/**
	 * When enabled, indicates that it is safe to cache {@link jakarta.transaction.UserTransaction}
	 * references in the {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform}
	 *
	 * @settingDefault Generally {@code true}, though {@code JtaPlatform} implementations
	 * can do their own thing.
	 *
	 * @since 4.0
	 */
	String JTA_CACHE_UT = "hibernate.jta.cacheUserTransaction";

	/**
	 * A transaction can be rolled back by another thread ("tracking by thread")
	 * -- not the original application. Examples of this include a JTA
	 * transaction timeout handled by a background reaper thread.  The ability
	 * to handle this situation requires checking the Thread ID every time
	 * Session is called.  This can certainly have performance considerations.
	 *
	 * @settingDefault {@code true} (enabled).
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyJtaTrackingByThread(boolean)
	 */
	String JTA_TRACK_BY_THREAD = "hibernate.jta.track_by_thread";

	/**
	 * When enabled, allows access to the {@link org.hibernate.Transaction} even when
	 * using a JTA for transaction management.
	 * <p>
	 * Values are {@code true}, which grants access, and {@code false}, which does not.
	 * <p>
	 *
	 * @settingDefault {@code false} when bootstrapped via JPA; {@code true} otherwise.
	 *
	 * @see JpaComplianceSettings#JPA_TRANSACTION_COMPLIANCE
	 */
	String ALLOW_JTA_TRANSACTION_ACCESS = "hibernate.jta.allowTransactionAccess";

	/**
	 * When enabled, specifies that the {@link org.hibernate.Session} should be
	 * closed automatically at the end of each transaction.
	 *
	 * @settingDefault {@code false}
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyAutoClosing(boolean)
	 */
	String AUTO_CLOSE_SESSION = "hibernate.transaction.auto_close_session";

	/**
	 * When enabled, specifies that automatic flushing should occur during the JTA
	 * {@link jakarta.transaction.Synchronization#beforeCompletion()} callback.
	 *
	 * @settingDefault {@code true} unless using JPA bootstrap
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyAutoFlushing(boolean)
	 */
	String FLUSH_BEFORE_COMPLETION = "hibernate.transaction.flush_before_completion";

	/**
	 * Allows a detached proxy or lazy collection to be fetched even when not
	 * associated with an open persistence context, by creating a temporary
	 * persistence context when the proxy or collection is accessed. This
	 * behavior is not recommended, since it can easily break transaction
	 * isolation or lead to data aliasing; it is therefore disabled by default.
	 *
	 * @settingDefault {@code false} (disabled)
	 *
	 * @apiNote Generally speaking, all access to transactional data should be done in a transaction.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyLazyInitializationOutsideTransaction(boolean)
	 */
	@Unsafe
	String ENABLE_LAZY_LOAD_NO_TRANS = "hibernate.enable_lazy_load_no_trans";

	/**
	 * When enabled, allows update operations outside a transaction.
	 * <p>
	 * Since version 5.2 Hibernate conforms with the JPA specification and disallows
	 * flushing any update outside a transaction.
	 * <p>
	 * Values are {@code true}, which allows flushing outside a transaction, and
	 * {@code false}, which does not.
	 * <p>
	 * The default behavior is to disallow update operations outside a transaction.
	 *
	 * @settingDefault {@code false} (disabled)
	 *
	 * @apiNote Generally speaking, all access to transactional data should be done in a transaction.
	 * Combining this with second-level caching, e.g., will cause problems.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#allowOutOfTransactionUpdateOperations(boolean)
	 *
	 * @since 5.2
	 */
	@Unsafe
	String ALLOW_UPDATE_OUTSIDE_TRANSACTION = "hibernate.allow_update_outside_transaction";
}
