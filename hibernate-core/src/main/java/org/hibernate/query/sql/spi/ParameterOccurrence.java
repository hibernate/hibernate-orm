/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sql.spi;

import org.hibernate.query.spi.QueryParameterImplementor;

/**
 * @author Christian Beikov
 */
public final class ParameterOccurrence {

	private final QueryParameterImplementor<?> parameter;
	private final int sourcePosition;

	public ParameterOccurrence(QueryParameterImplementor<?> parameter, int sourcePosition) {
		this.parameter = parameter;
		this.sourcePosition = sourcePosition;
	}

	public QueryParameterImplementor<?> getParameter() {
		return parameter;
	}

	public int getSourcePosition() {
		return sourcePosition;
	}
}
