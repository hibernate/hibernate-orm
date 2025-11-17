/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.HibernateException;

/**
 * Indicates a problem accepting/executing a schema management command.
 *
 * @author Steve Ebersole
 */
public class CommandAcceptanceException extends HibernateException {
	public CommandAcceptanceException(String message) {
		super( message );
	}

	public CommandAcceptanceException(String message, Throwable cause) {
		super( message, cause );
	}
}
