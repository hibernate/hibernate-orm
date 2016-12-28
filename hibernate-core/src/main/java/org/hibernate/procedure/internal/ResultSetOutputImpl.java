/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.procedure.internal;

import java.util.List;

import org.hibernate.procedure.ResultSetOutput;


/**
 * Implementation of ResultSetOutput
 *
 * @author Steve Ebersole
 */
public class ResultSetOutputImpl implements ResultSetOutput {
	private final List results;

	public ResultSetOutputImpl(List results) {
		this.results = results;
	}

	@Override
	public boolean isResultSet() {
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List getResultList() {
		return results;
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
