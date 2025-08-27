/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.HibernateException;

/**
 * Indicates a problem in performing schema management.
 * <p>
 * Specifically this represents a problem of an infrastructural nature. For
 * problems applying a specific command see {@link CommandAcceptanceException}
 *
 * @author Steve Ebersole
 */
public class SchemaManagementException extends HibernateException {
	public SchemaManagementException(String message) {
		super( message );
	}

	public SchemaManagementException(String message, Throwable root) {
		super( message, root );
	}
}
