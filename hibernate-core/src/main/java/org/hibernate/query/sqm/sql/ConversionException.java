/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
