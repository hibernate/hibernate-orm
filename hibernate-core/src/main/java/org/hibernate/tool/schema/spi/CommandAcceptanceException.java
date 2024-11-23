/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
