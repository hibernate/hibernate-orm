/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm;

/**
 * The root exception for errors (potential bugs) in the sqm parser code itself, as opposed
 * to {@link QueryException} which indicates problems with the sqm.
 *
 * @author Steve Ebersole
 */
public class ParsingException extends RuntimeException {
	public ParsingException(String message) {
		super( message );
	}

	public ParsingException(String message, Throwable cause) {
		super( message, cause );
	}
}
