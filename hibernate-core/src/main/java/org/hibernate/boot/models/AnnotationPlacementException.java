/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models;

import org.hibernate.MappingException;

/**
 * @author Steve Ebersole
 */
public class AnnotationPlacementException extends MappingException {
	public AnnotationPlacementException(String message) {
		super( message );
	}

	public AnnotationPlacementException(String message, Throwable cause) {
		super( message, cause );
	}
}
