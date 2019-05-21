/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate;

/**
 * Thrown from methods added for 6.0 that are not yet implemented.
 *
 * todo (6.0) : prior going final, we need to find all usages of this and implement all methods (or throw a different exception)
 */
public class NotYetImplementedFor6Exception extends RuntimeException {
	public NotYetImplementedFor6Exception(String message) {
		super( message );
	}

	public NotYetImplementedFor6Exception(Class clazz) {
		super( clazz.getName() );
	}

	public NotYetImplementedFor6Exception() {
		super( "Not yet implemented" );
	}
}
