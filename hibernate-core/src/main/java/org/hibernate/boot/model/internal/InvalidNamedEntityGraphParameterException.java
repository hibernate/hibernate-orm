/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;


import org.hibernate.AnnotationException;

/**
 * Thrown by {@link NamedGraphCreatorParsed} to indicate an issue with the parameters provided
 * to a {@code @NamedEntityGraph} annotation.
 *
 */
public class InvalidNamedEntityGraphParameterException extends AnnotationException {
	private static final long serialVersionUID = 1L;

	public InvalidNamedEntityGraphParameterException(String message) {
		super( message );
	}
}
