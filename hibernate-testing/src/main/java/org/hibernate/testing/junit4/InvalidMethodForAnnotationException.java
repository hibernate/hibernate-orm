/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
