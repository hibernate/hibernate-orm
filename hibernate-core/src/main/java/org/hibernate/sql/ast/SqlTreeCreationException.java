/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast;

import org.hibernate.HibernateException;

/**
 * Base exception type for problems building a SQL tree.
 *
 * @author Steve Ebersole
 */
public class SqlTreeCreationException extends HibernateException {
	public SqlTreeCreationException(String message) {
		super( message );
	}

	public SqlTreeCreationException(String message, Throwable cause) {
		super( message, cause );
	}
}
