/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Indicates an attempt was made to use a closed resource, such as
 * a closed {@link Session} or {@link SessionFactory}.
 *
 * @author Steve Ebersole
 */
public class ResourceClosedException extends HibernateException {
	/**
	 * Constructs a ResourceClosedException using the supplied message.
	 *
	 * @param message The message explaining the exception condition
	 */
	public ResourceClosedException(String message) {
		super( message );
	}
}
