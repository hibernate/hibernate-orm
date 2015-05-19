/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import org.hibernate.AnnotationException;

/**
 * An exception that indicates a condition where the hope is that subsequent processing will be able to
 * recover from it.
 *
 * @deprecated Was only ever referenced in a single place, in an extremely dubious way.
 *
 * @author Emmanuel Bernard
 */
@Deprecated
public class RecoverableException extends AnnotationException {
	/**
	 * Constructs a RecoverableException using the given message and underlying cause.
	 *
	 * @param msg The message explaining the condition that caused the exception
	 * @param cause The underlying exception
	 */
	public RecoverableException(String msg, Throwable cause) {
		super( msg, cause );
	}

	/**
	 * Constructs a RecoverableException using the given message and underlying cause.
	 *
	 * @param msg
	 */
	public RecoverableException(String msg) {
		super( msg );
	}
}
