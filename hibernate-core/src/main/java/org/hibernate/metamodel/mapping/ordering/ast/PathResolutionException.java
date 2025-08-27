/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.mapping.NonTransientException;

/**
 * Indicates a problem resolving a domain-path occurring in an order-by fragment
 *
 * @author Steve Ebersole
 */
public class PathResolutionException extends HibernateException implements NonTransientException {
	public PathResolutionException(String message) {
		super( message );
	}

	public PathResolutionException(String message, Throwable cause) {
		super( message, cause );
	}
}
