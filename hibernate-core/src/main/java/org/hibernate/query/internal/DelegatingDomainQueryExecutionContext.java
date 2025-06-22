/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

	public DelegatingDomainQueryExecutionContext(DomainQueryExecutionContext delegate) {
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
