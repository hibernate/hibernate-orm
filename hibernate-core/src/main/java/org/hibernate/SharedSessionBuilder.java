/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.sql.Connection;

/**
 * Specialized {@link SessionBuilder} with access to stuff from another session.
 *
 * @author Steve Ebersole
 */
public interface SharedSessionBuilder<T extends SharedSessionBuilder> extends SessionBuilder<T> {

	/**
	 * Signifies that the transaction context from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Use {@link #connection()} instead
	 */
	@Deprecated
	default T transactionContext() {
		return connection();
	}

	/**
	 * Signifies that the connection from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	T connection();

	/**
	 * Signifies the interceptor from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	T interceptor();

	/**
	 * Signifies that the connection release mode from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (snce 6.0) use {@link #connectionHandlingMode} instead.
	 */
	@Deprecated
	T connectionReleaseMode();

	/**
	 * Signifies that the connection release mode from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	T connectionHandlingMode();

	/**
	 * Signifies that the autoJoinTransaction flag from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	T autoJoinTransactions();

	/**
	 * Signifies that the FlushMode from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	T flushMode();

	/**
	 * Signifies that the autoClose flag from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	T autoClose();

	/**
	 * Signifies that the flushBeforeCompletion flag from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #flushMode()} instead.
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default T flushBeforeCompletion() {
		flushMode();
		return (T) this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// overrides to maintain binary compatibility

	@Override
	T interceptor(Interceptor interceptor);

	@Override
	T noInterceptor();

	@Override
	T connection(Connection connection);

	@Override
	T connectionReleaseMode(ConnectionReleaseMode connectionReleaseMode);

	@Override
	T autoJoinTransactions(boolean autoJoinTransactions);

	@Override
	T autoClose(boolean autoClose);

	@Override
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
