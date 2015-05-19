/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
