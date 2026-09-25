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
