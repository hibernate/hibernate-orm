/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
class ResultSetOutputImpl implements ResultSetOutput {
	private final Supplier<List<?>> resultSetSupplier;

	public ResultSetOutputImpl(List<?> results) {
		this.resultSetSupplier = () -> results;
	}

	public ResultSetOutputImpl(Supplier<List<?>> resultSetSupplier) {
		this.resultSetSupplier = resultSetSupplier;
	}

	@Override
	public boolean isResultSet() {
		return true;
	}

	@Override
	public List<?> getResultList() {
		return resultSetSupplier.get();
	}

	@Override
	public Object getSingleResult() {
		final List<?> results = getResultList();
		if ( results == null || results.isEmpty() ) {
			return null;
		}
		else {
			return results.get( 0 );
		}
	}
}
