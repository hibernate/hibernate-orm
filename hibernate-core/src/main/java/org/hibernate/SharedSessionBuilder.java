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
	 * Signifies the interceptor from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	public SharedSessionBuilder interceptor();

	/**
	 * Signifies that the connection from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	public SharedSessionBuilder connection();

	/**
	 * Signifies that the connection release mode from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	public SharedSessionBuilder connectionReleaseMode();

	/**
	 * Signifies that the autoJoinTransaction flag from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	public SharedSessionBuilder autoJoinTransactions();

	/**
	 * Signifies that the autoClose flag from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated For same reasons as {@link SessionBuilder#autoClose(boolean)} was deprecated.  However, shared
	 * session builders can use {@link #autoClose(boolean)} since they do not "inherit" the owner.
	 */
	@Deprecated
	public SharedSessionBuilder autoClose();

	/**
	 * Signifies that the flushBeforeCompletion flag from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	public SharedSessionBuilder flushBeforeCompletion();

	/**
	 * Signifies that the transaction context from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	public SharedSessionBuilder transactionContext();

	@Override
	SharedSessionBuilder interceptor(Interceptor interceptor);

	@Override
	SharedSessionBuilder noInterceptor();

	@Override
	SharedSessionBuilder connection(Connection connection);

	@Override
	SharedSessionBuilder connectionReleaseMode(ConnectionReleaseMode connectionReleaseMode);

	@Override
	SharedSessionBuilder autoJoinTransactions(boolean autoJoinTransactions);

	@Override
	SharedSessionBuilder autoClose(boolean autoClose);

	@Override
	SharedSessionBuilder flushBeforeCompletion(boolean flushBeforeCompletion);
}
