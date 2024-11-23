/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
