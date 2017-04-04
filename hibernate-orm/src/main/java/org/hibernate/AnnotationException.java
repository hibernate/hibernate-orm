/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Annotation related exception.
 *
 * The EJB3 EG will probably set a generic exception.  I'll then use this one.
 *
 * @author Emmanuel Bernard
 */
public class AnnotationException extends MappingException {
	/**
	 * Constructs an AnnotationException using the given message and cause.
	 *
	 * @param msg The message explaining the reason for the exception.
	 * @param cause The underlying cause.
	 */
	public AnnotationException(String msg, Throwable cause) {
		super( msg, cause );
	}

	/**
	 * Constructs an AnnotationException using the given message.
	 *
	 * @param msg The message explaining the reason for the exception.
	 */
	public AnnotationException(String msg) {
		super( msg );
	}
}
