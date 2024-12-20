/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.spi;

import org.hibernate.HibernateException;

/**
 * @author Steve Ebersole
 */
public class SchemaExtractionException extends HibernateException {
	public SchemaExtractionException(String message) {
		super( message );
	}

	public SchemaExtractionException(String message, Throwable root) {
		super( message, root );
	}
}
