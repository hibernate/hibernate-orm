/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce;

import org.hibernate.HibernateException;

/**
 * Represents a problem in the converter itself (Hibernate bug) during conversion
 * of a SQM into SQL interpretation
 * <p/>
 * This differs from syntax problems in the query which is reported
 * as {@link SyntaxException}.
 *
 * @author Steve Ebersole
 */
public class ConversionException extends HibernateException {
	public ConversionException(String message) {
		this( message, null );
	}

	public ConversionException(Throwable cause) {
		this( "uncategorized", cause );
	}

	public ConversionException(String message, Throwable cause) {
		super( "A problem occurred in the SQM interpreter : " + message, cause );
	}
}
