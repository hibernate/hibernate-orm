/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.junit4;


/**
 * Indicates that an annotations was attached to a method incorrectly.
 *
 * @author Steve Ebersole
 */
public class InvalidMethodForAnnotationException extends RuntimeException {
	public InvalidMethodForAnnotationException(String message) {
		super( message );
	}

	public InvalidMethodForAnnotationException(String message, Throwable cause) {
		super( message, cause );
	}
}
