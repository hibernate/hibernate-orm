/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
