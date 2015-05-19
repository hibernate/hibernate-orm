/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Thrown if Hibernate can't instantiate a class at runtime.
 *
 * @author Gavin King
 */
public class InstantiationException extends HibernateException {
	private final Class clazz;

	/**
	 * Constructs a InstantiationException.
	 *
	 * @param message A message explaining the exception condition
	 * @param clazz The Class we are attempting to instantiate
	 * @param cause The underlying exception
	 */
	public InstantiationException(String message, Class clazz, Throwable cause) {
		super( message, cause );
		this.clazz = clazz;
	}

	/**
	 * Constructs a InstantiationException.
	 *
	 * @param message A message explaining the exception condition
	 * @param clazz The Class we are attempting to instantiate
	 */
	public InstantiationException(String message, Class clazz) {
		this( message, clazz, null );
	}

	/**
	 * Constructs a InstantiationException.
	 *
	 * @param message A message explaining the exception condition
	 * @param clazz The Class we are attempting to instantiate
	 * @param cause The underlying exception
	 */
	public InstantiationException(String message, Class clazz, Exception cause) {
		super( message, cause );
		this.clazz = clazz;
	}

	/**
	 * Returns the Class we were attempting to instantiate.
	 *
	 * @deprecated Use {@link #getUninstantiatableClass} instead
	 *
	 * @return The class we are unable to instantiate
	 */
	@Deprecated
	public Class getPersistentClass() {
		return clazz;
	}

	/**
	 * Returns the Class we were attempting to instantiate.
	 *
	 * @return The class we are unable to instantiate
	 */
	public Class getUninstantiatableClass() {
		return clazz;
	}

	@Override
	public String getMessage() {
		return super.getMessage() + " : " + clazz.getName();
	}

}
