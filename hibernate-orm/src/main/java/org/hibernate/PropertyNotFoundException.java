/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Indicates that an expected getter or setter method could not be
 * found on a class.
 *
 * @author Gavin King
 */
public class PropertyNotFoundException extends MappingException {
	/**
	 * Constructs a PropertyNotFoundException given the specified message.
	 *
	 * @param message A message explaining the exception condition
	 */
	public PropertyNotFoundException(String message) {
		super( message );
	}
}
