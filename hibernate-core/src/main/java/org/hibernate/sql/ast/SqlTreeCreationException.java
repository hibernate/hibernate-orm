/*
 * SPDX-License-Identifier: Apache-2.0
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
