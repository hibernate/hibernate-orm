/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.HibernateException;

/**
 * Indicates a problem
 * @author Steve Ebersole
 */
public class SqlScriptException extends HibernateException {
	public SqlScriptException(String message) {
		super( message );
	}

	public SqlScriptException(String message, Throwable cause) {
		super( message, cause );
	}
}
