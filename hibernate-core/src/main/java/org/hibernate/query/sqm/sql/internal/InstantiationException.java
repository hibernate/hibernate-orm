/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.HibernateException;

/**
 * Indicates a problem performing a dynamic instantiation
 *
 * @author Steve Ebersole
 */
public class InstantiationException extends HibernateException {
	public InstantiationException(String message) {
		super( message );
	}

	public InstantiationException(String message, Throwable cause) {
		super( message, cause );
	}
}
