/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.proxy.pojo.javassist;
import java.io.Serializable;
import java.lang.reflect.Method;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyObject;
import javassist.util.proxy.ProxyFactory;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.proxy.AbstractSerializableProxy;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.pojo.BasicLazyInitializer;
import org.hibernate.type.CompositeType;

/**
 * Serializable placeholder for Javassist proxies
 */
public final class SerializableProxy extends AbstractSerializableProxy {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( SerializableProxy.class );

	private static final MethodFilter FINALIZE_FILTER = new MethodFilter() {
		public boolean isHandled(Method m) {
			// skip finalize methods
			return !( m.getParameterTypes().length == 0 && m.getName().equals( "finalize" ) );
		}
	};

	private Class persistentClass;
	private Class[] interfaces;
	private Class getIdentifierMethodClass;
	private Class setIdentifierMethodClass;
	private String getIdentifierMethodName;
	private String setIdentifierMethodName;
	private Class[] setIdentifierMethodParams;
	private CompositeType componentIdType;

	public SerializableProxy() {
	}

	public SerializableProxy(
			final String entityName,
			final Class persistentClass,
			final Class[] interfaces,
			final Serializable id,
			final Boolean readOnly,
			final Method getIdentifierMethod,
			final Method setIdentifierMethod,
			CompositeType componentIdType) {
		super( entityName, id, readOnly );
		this.persistentClass = persistentClass;
		this.interfaces = interfaces;
		if (getIdentifierMethod!=null) {
			getIdentifierMethodClass = getIdentifierMethod.getDeclaringClass();
			getIdentifierMethodName = getIdentifierMethod.getName();
		}
		if (setIdentifierMethod!=null) {
			setIdentifierMethodClass = setIdentifierMethod.getDeclaringClass();
			setIdentifierMethodName = setIdentifierMethod.getName();
			setIdentifierMethodParams = setIdentifierMethod.getParameterTypes();
		}
		this.componentIdType = componentIdType;
	}

	private Object readResolve() {
		try {
			HibernateProxy proxy = getProxy(
					getEntityName(),
					persistentClass,
					interfaces,
					getIdentifierMethodName == null
					? null
					: getIdentifierMethodClass.getDeclaredMethod( getIdentifierMethodName, (Class[]) null ),
					setIdentifierMethodName == null
					? null
					: setIdentifierMethodClass.getDeclaredMethod( setIdentifierMethodName, setIdentifierMethodParams ),
					componentIdType,
					getId(),
					null
										   );
			setReadOnlyBeforeAttachedToSession( ( JavassistLazyInitializer ) proxy.getHibernateLazyInitializer() );
			return proxy;
		}
		catch (NoSuchMethodException nsme) {
			throw new HibernateException("could not create proxy for entity: " + getEntityName(), nsme);
		}
	}

	private HibernateProxy getProxy(
			final String entityName,
			final Class persistentClass,
			final Class[] interfaces,
			final Method getIdentifierMethod,
			final Method setIdentifierMethod,
			CompositeType componentIdType,
			final Serializable id,
			final SessionImplementor session) throws HibernateException {
		// note: interface is assumed to already contain HibernateProxy.class
		try {
			final BasicLazyInitializer lazyInitailizer = new JavassistLazyInitializer(
					entityName,
					persistentClass,
					interfaces,
					id,
					getIdentifierMethod,
					setIdentifierMethod,
					componentIdType,
					session,
					ReflectHelper.overridesEquals( persistentClass ) );
			final MethodHandler instance = new JavassistMethodHandler( true, lazyInitailizer );
			ProxyFactory factory = new ProxyFactory();
			factory.setSuperclass( interfaces.length == 1 ? persistentClass : null );
			factory.setInterfaces( interfaces );
			factory.setFilter( FINALIZE_FILTER );
			Class cl = factory.createClass();
			final HibernateProxy proxy = ( HibernateProxy ) cl.newInstance();
			( (ProxyObject) proxy ).setHandler( instance );
			return proxy;
		}
		catch ( Throwable t ) {
			LOG.error(LOG.javassistEnhancementFailed(entityName), t);
			throw new HibernateException(LOG.javassistEnhancementFailed(entityName), t);
		}
	}
}
