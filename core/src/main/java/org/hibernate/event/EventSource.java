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
package org.hibernate.event;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.engine.ActionQueue;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Gavin King
 */
public interface EventSource extends SessionImplementor, Session {
	
	/**
	 * Get the ActionQueue for this session
	 */
	public ActionQueue getActionQueue();

	/**
	 * Instantiate an entity instance, using either an interceptor,
	 * or the given persister
	 */
	public Object instantiate(EntityPersister persister, Serializable id) throws HibernateException;

	/**
	 * Force an immediate flush
	 */
	public void forceFlush(EntityEntry e) throws HibernateException;

	/**
	 * Cascade merge an entity instance
	 */
	public void merge(String entityName, Object object, Map copiedAlready) throws HibernateException;
	/**
	 * Cascade persist an entity instance
	 */
	public void persist(String entityName, Object object, Map createdAlready) throws HibernateException;

	/**
	 * Cascade persist an entity instance during the flush process
	 */
	public void persistOnFlush(String entityName, Object object, Map copiedAlready);
	/**
	 * Cascade refesh an entity instance
	 */
	public void refresh(Object object, Map refreshedAlready) throws HibernateException;
	/**
	 * Cascade copy an entity instance
	 */
	public void saveOrUpdateCopy(String entityName, Object object, Map copiedAlready) throws HibernateException;
	
	/**
	 * Cascade delete an entity instance
	 */
	public void delete(String entityName, Object child, boolean isCascadeDeleteEnabled, Set transientEntities);

}
