/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.spi;

import java.sql.Connection;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionEventListener;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Base class for {@link SessionBuilder} implementations that wish to implement only parts of that contract themselves
 * while forwarding other method invocations to a delegate instance.
 *
 * @author Gunnar Morling
 */
public class ForwardingSessionBuilder implements SessionBuilder {

	private final SessionBuilder delegate;

	public ForwardingSessionBuilder(SessionBuilder delegate) {
		this.delegate = delegate;
	}

	@Override
	public Session openSession() {
		return delegate.openSession();
	}

	@Override
	public SessionBuilder interceptor(Interceptor interceptor) {
		return delegate.interceptor(interceptor);
	}

	@Override
	public SessionBuilder noInterceptor() {
		return delegate.noInterceptor();
	}

	@Override
	public SessionBuilder statementInspector(StatementInspector statementInspector) {
		return delegate.statementInspector( statementInspector );
	}

	@Override
	public SessionBuilder connection(Connection connection) {
		return delegate.connection(connection);
	}

	@Override
	public SessionBuilder connectionReleaseMode(
			ConnectionReleaseMode connectionReleaseMode) {
		return delegate.connectionReleaseMode(connectionReleaseMode);
	}

	@Override
	public SessionBuilder autoJoinTransactions(boolean autoJoinTransactions) {
		return delegate.autoJoinTransactions(autoJoinTransactions);
	}

	@Override
	public SessionBuilder autoClose(boolean autoClose) {
		return delegate.autoClose(autoClose);
	}

	@Override
	public SessionBuilder flushBeforeCompletion(boolean flushBeforeCompletion) {
		return delegate.flushBeforeCompletion(flushBeforeCompletion);
	}

	@Override
	public SessionBuilder tenantIdentifier(String tenantIdentifier) {
		return delegate.tenantIdentifier(tenantIdentifier);
	}

	@Override
	public SessionBuilder eventListeners(SessionEventListener... listeners) {
		return delegate.eventListeners(listeners);
	}

	@Override
	public SessionBuilder clearEventListeners() {
		return delegate.clearEventListeners();
	}
}
