/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql;

import org.hibernate.HibernateException;

/**
 * Indicates a problem converting an SQM tree to a SQL AST
 *
 * @author Steve Ebersole
 */
public class ConversionException extends HibernateException {
	public ConversionException(String message) {
		super( message );
	}

	public ConversionException(String message, Throwable cause) {
		super( message, cause );
	}
}
