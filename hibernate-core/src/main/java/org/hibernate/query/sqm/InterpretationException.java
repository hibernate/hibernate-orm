/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm;

/**
 * Represents a general uncaught problem performing the interpretation.  This might indicate
 * a semantic (user sqm) problem or a bug in the parser.
 *
 * @author Steve Ebersole
 */
public class InterpretationException extends RuntimeException {
	public InterpretationException(String query, Throwable cause) {
		super(
				"Error interpreting query [" + query + "]; this may indicate a semantic (user query) problem or a bug in the parser",
				cause
		);
	}
}
