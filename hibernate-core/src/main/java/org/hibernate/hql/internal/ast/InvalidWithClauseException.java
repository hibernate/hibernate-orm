/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast;

/**
 * Indicates an issue with the encountered with-clause.
 *
 * @author Steve Ebersole
 */
public class InvalidWithClauseException extends QuerySyntaxException {
	public InvalidWithClauseException(String message, String queryString) {
		super( message, queryString );
		if ( queryString == null ) {
			throw new IllegalArgumentException( "Illegal to pass null as queryString argument" );
		}
	}
}
