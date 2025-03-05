/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * An exception that occurs while reading mapping annotations.
 *
 * @author Emmanuel Bernard
 */
public class AnnotationException extends MappingException {
	/**
	 * Constructs an {@code AnnotationException} using the given message and cause.
	 *
	 * @param msg The message explaining the reason for the exception.
	 * @param cause The underlying cause.
	 */
	public AnnotationException(String msg, Throwable cause) {
		super( msg, cause );
	}

	/**
	 * Constructs an {@code AnnotationException} using the given message.
	 *
	 * @param msg The message explaining the reason for the exception.
	 */
	public AnnotationException(String msg) {
		super( msg );
	}
}
