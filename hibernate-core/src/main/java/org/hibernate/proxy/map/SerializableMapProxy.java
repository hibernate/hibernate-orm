/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy.map;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.hibernate.proxy.AbstractSerializableProxy;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyFactory;
import org.hibernate.type.CompositeType;

public final class SerializableMapProxy extends AbstractSerializableProxy {

	public SerializableMapProxy(
			String entityName,
			Serializable id,
			Boolean readOnly,
			String sessionFactoryUuid,
			boolean allowLoadOutsideTransaction) {
		super( entityName, id, readOnly, sessionFactoryUuid, allowLoadOutsideTransaction );
	}

	private Object readResolve() {
		MapLazyInitializer initializer = new MapLazyInitializer( getEntityName(), getId(), null );
		afterDeserialization( initializer );
		return new MapProxy( initializer );
	}
}
