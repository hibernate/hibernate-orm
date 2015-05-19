/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * An exception that occurs while reading mapping sources (xml/annotations),usually as a result of something
 * screwy in the O-R mappings.
 *
 * @author Gavin King
 */
public class MappingException extends HibernateException {
	/**
	 * Constructs a MappingException using the given information.
	 *
	 * @param message A message explaining the exception condition
	 * @param cause The underlying cause
	 */
	public MappingException(String message, Throwable cause) {
		super( message, cause );
	}

	/**
	 * Constructs a MappingException using the given information.
	 *
	 * @param cause The underlying cause
	 */
	public MappingException(Throwable cause) {
		super( cause );
	}

	/**
	 * Constructs a MappingException using the given information.
	 *
	 * @param message A message explaining the exception condition
	 */
	public MappingException(String message) {
		super( message );
	}

}






