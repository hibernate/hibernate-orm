/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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

import java.io.Serializable;

/**
 * Loads an entity by its primary identifier
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
public interface IdentifierLoadAccess<T> {
	/**
	 * Set the {@link LockOptions} to use when retrieving the entity.
	 */
	public IdentifierLoadAccess<T> with(LockOptions lockOptions);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * assuming that the instance exists. This method might return a proxied instance that
	 * is initialized on-demand, when a non-identifier method is accessed. <br>
	 * <br>
	 * You should not use this method to determine if an instance exists (use <tt>get()</tt> instead). Use this only to
	 * retrieve an instance that you assume exists, where non-existence
	 * would be an actual error. <br>
	 * <br>
	 * Due to the nature of the proxy functionality the return type of this method cannot use
	 * the generic type.
	 * 
	 * @param theClass
	 *            a persistent class
	 * @param id
	 *            a valid identifier of an existing persistent instance of the class
	 * @return the persistent instance or proxy
	 * @throws HibernateException
	 */
	public Object getReference(Serializable id);

	/**
	 * Return the persistent instance of the given entity class with the given identifier,
	 * or null if there is no such persistent instance. (If the instance is already associated
	 * with the session, return that instance. This method never returns an uninitialized instance.)
	 * 
	 * @param clazz
	 *            a persistent class
	 * @param id
	 *            an identifier
	 * @return a persistent instance or null
	 * @throws HibernateException
	 */
	public Object load(Serializable id);
}
