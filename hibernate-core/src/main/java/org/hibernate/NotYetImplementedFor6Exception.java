/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate;

import org.hibernate.metamodel.mapping.NonTransientException;

/**
 * Thrown from methods added for 6.0 that are not yet implemented.
 *
 * @deprecated As of 6.2 all uses of this exception have been removed;  and it
 * is completely used in the Hibernate code
 */
@Internal
@Deprecated(since = "6.2", forRemoval = true)
public class NotYetImplementedFor6Exception extends RuntimeException implements NonTransientException,
		NotImplementedYetException {
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
