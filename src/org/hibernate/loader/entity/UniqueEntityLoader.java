//$Id$
package org.hibernate.loader.entity;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;

/**
 * Loads entities for a <tt>EntityPersister</tt>
 * @author Gavin King
 */
public interface UniqueEntityLoader {
	/**
	 * Load an entity instance. If <tt>optionalObject</tt> is supplied,
	 * load the entity state into the given (uninitialized) object.
	 */
	public Object load(Serializable id, Object optionalObject, SessionImplementor session) throws HibernateException;
}






