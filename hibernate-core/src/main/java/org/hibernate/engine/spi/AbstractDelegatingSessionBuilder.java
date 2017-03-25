/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.sql.Connection;
import java.util.TimeZone;

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
public abstract class AbstractDelegatingSessionBuilder implements SessionBuilder {

	private final SessionBuilder delegate;

	public AbstractDelegatingSessionBuilder(SessionBuilder delegate) {
		this.delegate = delegate;
	}

	@Override
	public Session openSession() {
		return delegate.openSession();
	}

	@Override
	public SessionBuilder interceptor(Interceptor interceptor) {
		delegate.interceptor( interceptor );
		return this;
	}

	@Override
	public SessionBuilder noInterceptor() {
		delegate.noInterceptor();
		return this;
	}

	@Override
	public SessionBuilder statementInspector(StatementInspector statementInspector) {
		delegate.statementInspector( statementInspector );
		return this;
	}

	@Override
	public SessionBuilder connection(Connection connection) {
		delegate.connection( connection );
		return this;
	}

	@Override
	public SessionBuilder connectionReleaseMode(ConnectionReleaseMode connectionReleaseMode) {
		delegate.connectionReleaseMode( connectionReleaseMode );
		return this;
	}

	@Override
	public SessionBuilder autoJoinTransactions(boolean autoJoinTransactions) {
		delegate.autoJoinTransactions( autoJoinTransactions );
		return this;
	}

	@Override
	public SessionBuilder autoClose(boolean autoClose) {
		delegate.autoClose( autoClose );
		return this;
	}

	@Override
	public SessionBuilder flushBeforeCompletion(boolean flushBeforeCompletion) {
		delegate.flushBeforeCompletion( flushBeforeCompletion );
		return this;
	}

	@Override
	public SessionBuilder tenantIdentifier(String tenantIdentifier) {
		delegate.tenantIdentifier( tenantIdentifier );
		return this;
	}

	@Override
	public SessionBuilder eventListeners(SessionEventListener... listeners) {
		delegate.eventListeners( listeners );
		return this;
	}

	@Override
	public SessionBuilder clearEventListeners() {
		delegate.clearEventListeners();
		return this;
	}

	@Override
	public SessionBuilder jdbcTimeZone(TimeZone timeZone) {
		delegate.jdbcTimeZone(timeZone);
		return this;
	}
}
