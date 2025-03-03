/*
 * SPDX-License-Identifier: Apache-2.0
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
