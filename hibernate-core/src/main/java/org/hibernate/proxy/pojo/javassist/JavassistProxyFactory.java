/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy.pojo.javassist;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.CompositeType;

/**
 * A {@link ProxyFactory} implementation for producing Javassist-based proxies.
 *
 * @author Muga Nishizawa
 */
public class JavassistProxyFactory implements ProxyFactory, Serializable {

	protected static final Class[] NO_CLASSES = new Class[0];
	private Class persistentClass;
	private String entityName;
	private Class[] interfaces;
	private Method getIdentifierMethod;
	private Method setIdentifierMethod;
	private CompositeType componentIdType;
	private Class factory;
	private boolean overridesEquals;

	@Override
	public void postInstantiate(
			final String entityName,
			final Class persistentClass,
			final Set interfaces,
			final Method getIdentifierMethod,
			final Method setIdentifierMethod,
			CompositeType componentIdType) throws HibernateException {
		this.entityName = entityName;
		this.persistentClass = persistentClass;
		this.interfaces = (Class[]) interfaces.toArray(NO_CLASSES);
		this.getIdentifierMethod = getIdentifierMethod;
		this.setIdentifierMethod = setIdentifierMethod;
		this.componentIdType = componentIdType;
		this.factory = JavassistLazyInitializer.getProxyFactory( persistentClass, this.interfaces );
		this.overridesEquals = ReflectHelper.overridesEquals(persistentClass);
	}

	@Override
	public HibernateProxy getProxy(
			Serializable id,
			SessionImplementor session) throws HibernateException {
		return JavassistLazyInitializer.getProxy(
				factory,
				entityName,
				persistentClass,
				interfaces,
				getIdentifierMethod,
				setIdentifierMethod,
				componentIdType,
				id,
				session,
				overridesEquals
		);
	}

}
