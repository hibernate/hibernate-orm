/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.result.internal;

import java.util.List;

import org.hibernate.result.ResultSetOutput;

/**
 * Implementation of ResultSetOutput
 *
 * @author Steve Ebersole
 */
class ResultSetOutputImpl implements ResultSetOutput {
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
