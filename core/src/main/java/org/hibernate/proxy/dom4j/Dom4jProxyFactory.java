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
package org.hibernate.proxy.dom4j;

import org.hibernate.HibernateException;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.engine.SessionImplementor;

import java.util.Set;
import java.lang.reflect.Method;
import java.io.Serializable;

/**
 * Builds proxies for "dom4j" entity representations.
 *
 * @author Steve Ebersole
 */
public class Dom4jProxyFactory implements ProxyFactory {

	private String entityName;

	/**
	 * Called immediately after instantiation
	 */
	public void postInstantiate(
	        String entityName,
	        Class persistentClass,
	        Set interfaces,
	        Method getIdentifierMethod,
	        Method setIdentifierMethod,
	        AbstractComponentType componentIdType) throws HibernateException {
		this.entityName = entityName;
	}

	/**
	 * Create a new proxy
	 */
	public HibernateProxy getProxy(Serializable id, SessionImplementor session) throws HibernateException {
		return new Dom4jProxy( new Dom4jLazyInitializer( entityName, id, session ) );
	}
}
