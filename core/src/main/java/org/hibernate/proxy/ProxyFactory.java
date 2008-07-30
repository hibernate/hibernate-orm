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
package org.hibernate.proxy;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.AbstractComponentType;

/**
 * Contract for run-time, proxy-based lazy initialization proxies.
 *
 * @author Gavin King
 */
public interface ProxyFactory {

	/**
	 * Called immediately after instantiation of this factory.
	 * <p/>
	 * Essentially equivalent to contructor injection, but contracted
	 * here via interface.
	 *
	 * @param entityName The name of the entity for which this factory should
	 * generate proxies.
	 * @param persistentClass The entity class for which to generate proxies;
	 * not always the same as the entityName.
	 * @param interfaces The interfaces to expose in the generated proxy;
	 * {@link HibernateProxy} is already included in this collection.
	 * @param getIdentifierMethod Reference to the identifier getter method;
	 * invocation on this method should not force initialization
	 * @param setIdentifierMethod Reference to the identifier setter method;
	 * invocation on this method should not force initialization
	 * @param componentIdType For composite identifier types, a reference to
	 * the {@link org.hibernate.type.ComponentType type} of the identifier
	 * property; again accessing the id should generally not cause
	 * initialization - but need to bear in mind <key-many-to-one/>
	 * mappings.
	 * @throws HibernateException Indicates a problem completing post
	 * instantiation initialization.
	 */
	public void postInstantiate(
			String entityName,
	        Class persistentClass,
	        Set interfaces,
	        Method getIdentifierMethod,
	        Method setIdentifierMethod,
	        AbstractComponentType componentIdType) throws HibernateException;

	/**
	 * Create a new proxy instance
	 *
	 * @param id The id value for the proxy to be generated.
	 * @param session The session to which the generated proxy will be
	 * associated.
	 * @return The generated proxy.
	 * @throws HibernateException Indicates problems generating the requested
	 * proxy.
	 */
	public HibernateProxy getProxy(Serializable id,SessionImplementor session) throws HibernateException;

}
