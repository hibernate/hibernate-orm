/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Specialized {@link SessionBuilder} with access to stuff from another session.
 *
 * @author Steve Ebersole
 */
public interface SharedSessionBuilder extends SessionBuilder<SharedSessionBuilder> {

	/**
	 * Signifies that the transaction context from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Use {@link #connection()} instead
	 */
	@Deprecated
	default SharedSessionBuilder transactionContext() {
		return connection();
	}

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
	 * @deprecated (snce 6.0) use {@link #connectionHandlingMode} instead.
	 */
	@Deprecated
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

	/**
	 * Signifies that the autoClose flag from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	SharedSessionBuilder autoClose();

	/**
	 * Signifies that the flushBeforeCompletion flag from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #flushMode()} instead.
	 */
	@Deprecated
	default SharedSessionBuilder flushBeforeCompletion() {
		flushMode();
		return this;
	}
}
