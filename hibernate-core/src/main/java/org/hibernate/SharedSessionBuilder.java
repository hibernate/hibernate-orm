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
