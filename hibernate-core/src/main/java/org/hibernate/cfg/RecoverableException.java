/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
