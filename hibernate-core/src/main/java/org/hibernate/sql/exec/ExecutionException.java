/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec;

import org.hibernate.HibernateError;

/**
 * Indicates an exception performing execution
 *
 * @author Steve Ebersole
 */
public class ExecutionException extends HibernateError {
	public ExecutionException(String message) {
		this( message, null );
	}

	public ExecutionException(String message, Throwable cause) {
		super( "A problem occurred in the SQL executor : " + message, cause );
	}
}
