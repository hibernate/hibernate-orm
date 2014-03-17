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
package org.hibernate;

import org.hibernate.xml.spi.Origin;
import org.hibernate.xml.spi.SourceType;

/**
 * Annotation related exception.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class AnnotationException extends org.hibernate.metamodel.source.spi.MappingException {
	/**
	 * Constructs an AnnotationException using the given message and cause.
	 *
	 * @param msg The message explaining the reason for the exception.
	 * @param cause The underlying cause.
	 */
	public AnnotationException(String msg, Throwable cause) {
		this( msg, cause, new Origin( SourceType.ANNOTATION, "unknown" ) );
	}

	/**
	 * Constructs an AnnotationException using the given message.
	 *
	 * @param msg The message explaining the reason for the exception.
	 */
	public AnnotationException(String msg) {
		this( msg, new Origin( SourceType.ANNOTATION, "unknown" ) );
	}

	public AnnotationException(String msg, Throwable cause, Origin origin) {
		super( msg, cause, origin );
	}

	public AnnotationException(String msg, Origin origin) {
		super( msg, origin );
	}
}
