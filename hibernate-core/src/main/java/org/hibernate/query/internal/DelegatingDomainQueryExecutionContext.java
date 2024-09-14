/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.exec.spi.Callback;

/**
 * @author Steve Ebersole
 */
public class DelegatingDomainQueryExecutionContext implements DomainQueryExecutionContext {
	private final DomainQueryExecutionContext delegate;

	public <R> DelegatingDomainQueryExecutionContext(DomainQueryExecutionContext delegate) {
		this.delegate = delegate;
	}

	@Override
	public QueryOptions getQueryOptions() {
		return delegate.getQueryOptions();
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return delegate.getQueryParameterBindings();
	}

	@Override
	public Callback getCallback() {
		return delegate.getCallback();
	}

	@Override
	public boolean hasCallbackActions() {
		return delegate.hasCallbackActions();
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		return delegate.getSession();
	}
}
