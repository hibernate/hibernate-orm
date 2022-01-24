/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.sql.Connection;
import java.util.TimeZone;

import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Allows creation of a new {@link Session} with specific options.
 * 
 * @author Steve Ebersole
 */
public interface SessionBuilder<T extends SessionBuilder> {
	/**
	 * Opens a session with the specified options.
	 *
	 * @return The session
	 */
	Session openSession();

	/**
	 * Adds a specific interceptor to the session options.
	 *
	 * @param interceptor The interceptor to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	T interceptor(Interceptor interceptor);

	/**
	 * Signifies that no {@link Interceptor} should be used.
	 * <p/>
	 * By default, if no {@code Interceptor} is explicitly specified, the
	 * {@code Interceptor} associated with the {@link SessionFactory} is
	 * inherited by the new {@link Session}.
	 * <p/>
	 * Calling {@link #interceptor(Interceptor)} with null has the same effect.
	 *
	 * @return {@code this}, for method chaining
	 */
	T noInterceptor();

	/**
	 * Applies the given {@link StatementInspector} to the session.
	 *
	 * @param statementInspector The StatementInspector to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	T statementInspector(StatementInspector statementInspector);

	/**
	 * Adds a specific connection to the session options.
	 *
	 * @param connection The connection to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	T connection(Connection connection);

	/**
	 * Signifies that the connection release mode from the original session
	 * should be used to create the new session.
	 *
	 * @param mode The connection handling mode to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	T connectionHandlingMode(PhysicalConnectionHandlingMode mode);

	/**
	 * Should the session built automatically join in any ongoing JTA transactions.
	 *
	 * @param autoJoinTransactions Should JTA transactions be automatically joined
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see jakarta.persistence.SynchronizationType#SYNCHRONIZED
	 */
	T autoJoinTransactions(boolean autoJoinTransactions);

	/**
	 * Should the session be automatically cleared on a failed transaction?
	 *
	 * @param autoClear Whether the Session should be automatically cleared
	 *
	 * @return {@code this}, for method chaining
	 */
	@SuppressWarnings("UnusedReturnValue")
	T autoClear(boolean autoClear);

	/**
	 * Specify the initial FlushMode to use for the opened Session
	 *
	 * @param flushMode The initial FlushMode to use for the opened Session
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see jakarta.persistence.PersistenceContextType
	 */
	T flushMode(FlushMode flushMode);

	/**
	 * Define the tenant identifier to be associated with the opened session.
	 *
	 * @param tenantIdentifier The tenant identifier.
	 *
	 * @return {@code this}, for method chaining
	 */
	T tenantIdentifier(String tenantIdentifier);

	/**
	 * Add one or more {@link SessionEventListener} instances to the list of
	 * listeners for the new session to be built.
	 *
	 * @param listeners The listeners to incorporate into the built Session
	 *
	 * @return {@code this}, for method chaining
	 */
	T eventListeners(SessionEventListener... listeners);

	/**
	 * Remove all listeners intended for the built session currently held here,
	 * including any auto-apply ones; in other words, start with a clean slate.
	 *
	 * {@code this}, for method chaining
	 */
	T clearEventListeners();

	T jdbcTimeZone(TimeZone timeZone);

	/**
	 * Should the session be automatically closed after transaction completion?
	 *
	 * @param autoClose Should the session be automatically closed
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see jakarta.persistence.PersistenceContextType
	 *
	 * @deprecated Only integrations can specify autoClosing behavior of
	 * individual sessions.
	 */
	@Deprecated
	T autoClose(boolean autoClose);

	/**
	 * Use a specific connection release mode for these session options.
	 *
	 * @param connectionReleaseMode The connection release mode to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated use {@link #connectionHandlingMode} instead
	 */
	@Deprecated(since = "5.2")
	T connectionReleaseMode(ConnectionReleaseMode connectionReleaseMode);

	/**
	 * Should the session be automatically flushed during the "before completion"
	 * phase of transaction handling.
	 *
	 * @param flushBeforeCompletion Should the session be automatically flushed
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated use {@link #flushMode(FlushMode)} instead.
	 */
	@Deprecated(since = "5.2")
	@SuppressWarnings("unchecked")
	default T flushBeforeCompletion(boolean flushBeforeCompletion) {
		if ( flushBeforeCompletion ) {
			flushMode( FlushMode.ALWAYS );
		}
		else {
			flushMode( FlushMode.MANUAL );
		}
		return (T) this;
	}
}
