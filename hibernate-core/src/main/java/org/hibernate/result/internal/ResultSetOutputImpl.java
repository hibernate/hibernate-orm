/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.result.internal;

import java.sql.ResultSet;
import java.util.List;
import java.util.function.Supplier;

import javax.enterprise.inject.spi.Producer;

import org.hibernate.result.ResultSetOutput;

/**
 * Implementation of ResultSetOutput
 *
 * @author Steve Ebersole
 */
class ResultSetOutputImpl implements ResultSetOutput {
	private final Supplier<List> resultSetSupplier;

	public ResultSetOutputImpl(List results) {
		this.resultSetSupplier = () -> results;
	}

	public ResultSetOutputImpl(Supplier<List> resultSetSupplier) {
		this.resultSetSupplier = resultSetSupplier;
	}

	@Override
	public boolean isResultSet() {
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List getResultList() {
		return resultSetSupplier.get();
	}

	@Override
	public Object getSingleResult() {
		final List results = getResultList();
		if ( results == null || results.isEmpty() ) {
			return null;
		}
		else {
			return results.get( 0 );
		}
	}
}
