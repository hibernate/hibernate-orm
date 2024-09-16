/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
