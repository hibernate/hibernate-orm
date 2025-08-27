/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results;

import org.hibernate.HibernateException;
import org.hibernate.sql.results.graph.DomainResult;

/**
 * Base for problems creating {@link DomainResult}
 * instances
 *
 * @author Steve Ebersole
 */
public class DomainResultCreationException extends HibernateException {
	public DomainResultCreationException(String message) {
		super( message );
	}

	public DomainResultCreationException(String message, Throwable cause) {
		super( message, cause );
	}
}
