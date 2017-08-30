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
import org.hibernate.FlushMode;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionEventListener;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Base class for {@link SessionBuilder} implementations that wish to implement only parts of that contract themselves
 * while forwarding other method invocations to a delegate instance.
 *
 * @author Gunnar Morling
 * @author Guillaume Smet
 */
public abstract class AbstractDelegatingSessionBuilder<T extends SessionBuilder> implements SessionBuilder<T> {

	private final SessionBuilder delegate;

	public AbstractDelegatingSessionBuilder(SessionBuilder delegate) {
		this.delegate = delegate;
	}

	@SuppressWarnings("unchecked")
	protected T getThis() {
		return (T) this;
	}

	protected SessionBuilder delegate() {
		return delegate;
	}

	@Override
	public Session openSession() {
		return delegate.openSession();
	}

	@Override
	public T interceptor(Interceptor interceptor) {
		delegate.interceptor( interceptor );
		return getThis();
	}

	@Override
	public T noInterceptor() {
		delegate.noInterceptor();
		return getThis();
	}

	@Override
	public T statementInspector(StatementInspector statementInspector) {
		delegate.statementInspector( statementInspector );
		return getThis();
	}

	@Override
	public T connection(Connection connection) {
		delegate.connection( connection );
		return getThis();
	}

	@SuppressWarnings("deprecation")
	@Override
	public T connectionReleaseMode(ConnectionReleaseMode connectionReleaseMode) {
		delegate.connectionReleaseMode( connectionReleaseMode );
		return getThis();
	}

	@Override
	public T autoJoinTransactions(boolean autoJoinTransactions) {
		delegate.autoJoinTransactions( autoJoinTransactions );
		return getThis();
	}

	@Override
	public T autoClose(boolean autoClose) {
		delegate.autoClose( autoClose );
		return getThis();
	}

	@SuppressWarnings("deprecation")
	@Override
	public T flushBeforeCompletion(boolean flushBeforeCompletion) {
		delegate.flushBeforeCompletion( flushBeforeCompletion );
		return getThis();
	}

	@Override
	public T tenantIdentifier(String tenantIdentifier) {
		delegate.tenantIdentifier( tenantIdentifier );
		return getThis();
	}

	@Override
	public T eventListeners(SessionEventListener... listeners) {
		delegate.eventListeners( listeners );
		return getThis();
	}

	@Override
	public T clearEventListeners() {
		delegate.clearEventListeners();
		return getThis();
	}

	@Override
	public T jdbcTimeZone(TimeZone timeZone) {
		delegate.jdbcTimeZone(timeZone);
		return getThis();
	}

	@Override
	public T setQueryParameterValidation(boolean enabled) {
		delegate.setQueryParameterValidation( enabled );
		return getThis();
	}

	@Override
	public T connectionHandlingMode(PhysicalConnectionHandlingMode mode) {
		delegate.connectionHandlingMode( mode );
		return getThis();
	}

	@Override
	public T autoClear(boolean autoClear) {
		delegate.autoClear( autoClear );
		return getThis();
	}

	@Override
	public T flushMode(FlushMode flushMode) {
		delegate.flushMode( flushMode );
		return getThis();
	}
}
