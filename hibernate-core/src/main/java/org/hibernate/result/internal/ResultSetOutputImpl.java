/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.result.internal;

import java.util.List;
import java.util.function.Supplier;

import org.hibernate.result.ResultSetOutput;

/**
 * Implementation of ResultSetOutput
 *
 * @author Steve Ebersole
 */
class ResultSetOutputImpl<T> implements ResultSetOutput<T> {
	private final Supplier<List<T>> resultSetSupplier;

	public ResultSetOutputImpl(List<T> results) {
		this.resultSetSupplier = () -> results;
	}

	public ResultSetOutputImpl(Supplier<List<T>> resultSetSupplier) {
		this.resultSetSupplier = resultSetSupplier;
	}

	@Override
	public boolean isResultSet() {
		return true;
	}

	@Override
	public List<T> getResultList() {
		return resultSetSupplier.get();
	}

	@Override
	public Object getSingleResult() {
		final List<?> results = getResultList();
		return results == null || results.isEmpty() ? null : results.get( 0 );
	}
}
