/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate;

import java.sql.Connection;

/**
 * Represents a consolidation of all session creation options into a builder style delegate.
 * 
 * @author Steve Ebersole
 */
public interface SessionBuilder {
	/**
	 * Opens a session with the specified options.
	 *
	 * @return The session
	 */
	public Session openSession();

	/**
	 * Adds a specific interceptor to the session options
	 *
	 * @param interceptor The interceptor to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	public SessionBuilder interceptor(Interceptor interceptor);

	/**
	 * Signifies that no {@link Interceptor} should be used.
	 * <p/>
	 * By default the {@link Interceptor} associated with the {@link SessionFactory} is passed to the
	 * {@link Session} whenever we open one without the user having specified a specific interceptor to
	 * use.
	 * <p/>
	 * Calling {@link #interceptor(Interceptor)} with null has the same net effect.
	 *
	 * @return {@code this}, for method chaining
	 */
	public SessionBuilder noInterceptor();

	/**
	 * Adds a specific connection to the session options
	 *
	 * @param connection The connection to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	public SessionBuilder connection(Connection connection);

	/**
	 * Use a specific connection release mode for these session options
	 *
	 * @param connectionReleaseMode The connection release mode to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	public SessionBuilder connectionReleaseMode(ConnectionReleaseMode connectionReleaseMode);

	/**
	 * Should the session built automatically join in any ongoing JTA transactions
	 *
	 * @param autoJoinTransactions Should JTA transactions be automatically joined
	 *
	 * @return {@code this}, for method chaining
	 */
	public SessionBuilder autoJoinTransactions(boolean autoJoinTransactions);

	/**
	 * Should the session be automatically closed after transaction completion
	 *
	 * @param autoClose Should the session be automatically closed
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Only integrations can specify autoClosing behavior of individual sessions.  See
	 * {@link org.hibernate.engine.spi.SessionOwner}
	 */
	@Deprecated
	public SessionBuilder autoClose(boolean autoClose);

	/**
	 * Should the session be automatically flushed during the "before completion" phase of transaction handling.
	 *
	 * @param flushBeforeCompletion Should the session be automatically flushed
	 *
	 * @return {@code this}, for method chaining
	 */
	public SessionBuilder flushBeforeCompletion(boolean flushBeforeCompletion);

	/**
	 * Define the tenant identifier to be associated with the opened session.
	 *
	 * @param tenantIdentifier The tenant identifier.
	 *
	 * @return {@code this}, for method chaining
	 */
	public SessionBuilder tenantIdentifier(String tenantIdentifier);
}
