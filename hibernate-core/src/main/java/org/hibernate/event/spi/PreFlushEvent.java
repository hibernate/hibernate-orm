/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.Incubating;
import org.hibernate.query.spi.QueryParameterBindings;

/**
 * An event that occurs just before arguments are bound to JDBC
 * parameters during execution of HQL. Gives Hibernate a chance
 * to persist any transient entities used as query parameter
 * arguments.
 *
 * @author Gavin King
 * @since 7.2
 */
@Incubating
public class PreFlushEvent extends AbstractSessionEvent {

	private boolean preFlushRequired;
	private final QueryParameterBindings parameterBindings;

	public PreFlushEvent(QueryParameterBindings parameterBindings, EventSource source) {
		super( source );
		this.parameterBindings = parameterBindings;
	}

	public QueryParameterBindings getParameterBindings() {
		return parameterBindings;
	}

	public boolean isPreFlushRequired() {
		return preFlushRequired;
	}

	public void setPreFlushRequired(boolean preFlushRequired) {
		this.preFlushRequired = preFlushRequired;
	}
}
