/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm;

import org.hibernate.QueryException;

/**
 * Represents a general uncaught problem performing the interpretation.  This might indicate
 * a semantic (user sqm) problem or a bug in the parser.
 *
 * @author Steve Ebersole
 */
public class InterpretationException extends QueryException {
	public InterpretationException(String query) {
		this( query, null );
	}

	public InterpretationException(String query, Exception cause) {
		super(
				"Error interpreting query [" + query + "]; this may indicate a semantic (user query) problem or a bug in the parser",
				query,
				cause
		);
	}
}
