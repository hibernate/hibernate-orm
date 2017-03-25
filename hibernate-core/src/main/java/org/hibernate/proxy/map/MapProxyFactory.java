/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy.map;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.CompositeType;

/**
 * @author Gavin King
 */
public class MapProxyFactory implements ProxyFactory {

	private String entityName;

	public void postInstantiate(
			final String entityName,
			final Class persistentClass,
			final Set interfaces,
			final Method getIdentifierMethod,
			final Method setIdentifierMethod,
			CompositeType componentIdType) throws HibernateException {
		this.entityName = entityName;

	}

	@Override
	public HibernateProxy getProxy(final Serializable id, final SharedSessionContractImplementor session) {
		return new MapProxy( new MapLazyInitializer( entityName, id, session ) );
	}

}
