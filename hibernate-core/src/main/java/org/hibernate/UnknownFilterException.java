/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Indicates a request against an unknown filter name.
 *
 * @author Gavin King
 *
 * @see org.hibernate.annotations.FilterDef
 * @see Session#enableFilter(String)
 */
public class UnknownFilterException extends HibernateException {
	private final String name;

	/**
	 * Constructs an {@code UnknownFilterException} for the given name.
	 *
	 * @param name The filter that was unknown.
	 */
	public UnknownFilterException(String name) {
		super( "No filter named '" + name + "'" );
		this.name = name;
	}

	/**
	 * The unknown filter name.
	 *
	 * @return The unknown filter name.
	 */
	public String getName() {
		return name;
	}
}
