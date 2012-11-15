/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.loader.entity;
import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Loads entities for a <tt>EntityPersister</tt>
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface UniqueEntityLoader {
	/**
	 * Load an entity instance. If <tt>optionalObject</tt> is supplied,
	 * load the entity state into the given (uninitialized) object.
	 *
	 * @deprecated use {@link #load(java.io.Serializable, Object, SessionImplementor, LockOptions)} instead.
	 */
	@SuppressWarnings( {"JavaDoc"})
	@Deprecated
	public Object load(Serializable id, Object optionalObject, SessionImplementor session) throws HibernateException;

	/**
	 * Load an entity instance by id.  If <tt>optionalObject</tt> is supplied (non-<tt>null</tt>,
	 * the entity state is loaded into that object instance instead of instantiating a new one.
	 *
	 * @param id The id to be loaded
	 * @param optionalObject The (optional) entity instance in to which to load the state
	 * @param session The session from which the request originated
	 * @param lockOptions The lock options.
	 *
	 * @return The loaded entity
	 *
	 * @throws HibernateException indicates problem performing the load.
	 */
	public Object load(Serializable id, Object optionalObject, SessionImplementor session, LockOptions lockOptions);
}
