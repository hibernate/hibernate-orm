/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm;

/**
 * @author Andrea Boriero
 */
public class AliasCollisionException extends SemanticException {
	public AliasCollisionException(String message) {
		super( message );
	}

	public AliasCollisionException(String message, Exception cause) {
		super( message, cause );
	}
}
