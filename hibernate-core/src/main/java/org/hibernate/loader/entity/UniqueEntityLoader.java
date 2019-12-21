/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.entity;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

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
	 * @deprecated use {@link #load(java.io.Serializable, Object, SharedSessionContractImplementor, LockOptions)} instead.
	 */
	@SuppressWarnings( {"JavaDoc"})
	@Deprecated
	Object load(Serializable id, Object optionalObject, SharedSessionContractImplementor session) throws HibernateException;

	default Object load(Serializable id, Object optionalObject, SharedSessionContractImplementor session, Boolean readOnly) throws HibernateException {
		return load( id, optionalObject, session );
	}

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
	Object load(
			Serializable id,
			Object optionalObject,
			SharedSessionContractImplementor session,
			LockOptions lockOptions);

	default Object load(
			Serializable id,
			Object optionalObject,
			SharedSessionContractImplementor session,
			LockOptions lockOptions,
			Boolean readOnly) {
		return load( id, optionalObject, session, lockOptions );
	}
}
