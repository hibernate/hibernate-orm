/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
