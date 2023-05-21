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
public interface SharedSessionBuilder extends SessionBuilder {

	/**
	 * Signifies that the connection from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	SharedSessionBuilder connection();

	/**
	 * Signifies the interceptor from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	SharedSessionBuilder interceptor();

	/**
	 * Signifies that the connection release mode from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated use {@link #connectionHandlingMode} instead.
	 */
	@Deprecated(since = "6.0")
	SharedSessionBuilder connectionReleaseMode();

	/**
	 * Signifies that the connection release mode from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	SharedSessionBuilder connectionHandlingMode();

	/**
	 * Signifies that the autoJoinTransaction flag from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	SharedSessionBuilder autoJoinTransactions();

	/**
	 * Signifies that the FlushMode from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	SharedSessionBuilder flushMode();

	SharedSessionBuilder defaultBatchFetchSize();

	SharedSessionBuilder subselectFetchEnabled();

	/**
	 * Signifies that the autoClose flag from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	SharedSessionBuilder autoClose();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// overrides to maintain binary compatibility

	@Override
	SharedSessionBuilder interceptor(Interceptor interceptor);

	@Override
	SharedSessionBuilder noInterceptor();

	@Override
	SharedSessionBuilder connection(Connection connection);

	@Override
	SharedSessionBuilder autoJoinTransactions(boolean autoJoinTransactions);

	@Override
	SharedSessionBuilder autoClose(boolean autoClose);

	@Override
	SharedSessionBuilder defaultBatchFetchSize(int batchSize);

	@Override
	SharedSessionBuilder subselectFetchEnabled(boolean enabled);
}
