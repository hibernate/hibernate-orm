/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm;

import org.hibernate.query.SemanticException;

/**
 * Indicates an attempt to use an SqmPath in an unsupported manner - e.g., an
 * attempt to de-reference a basic value
 *
 * @author Steve Ebersole
 */
public class IllegalPathUsageException extends SemanticException {
	public IllegalPathUsageException(String message) {
		super( message );
	}

	public IllegalPathUsageException(String message, Exception cause) {
		super( message, cause );
	}
}
