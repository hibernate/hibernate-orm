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
import java.util.Set;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyObject;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.proxy.pojo.BasicLazyInitializer;
import org.hibernate.type.CompositeType;

/**
 * A {@link ProxyFactory} implementation for producing Javassist-based proxies.
 *
 * @author Muga Nishizawa
 * @author Aleksander Dukhno
 */
public class JavassistProxyFactory implements ProxyFactory, Serializable {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( JavassistProxyFactory.class );

	private static final MethodFilter FINALIZE_FILTER = new MethodFilter() {
		public boolean isHandled(Method m) {
			// skip finalize methods
			return !( m.getParameterTypes().length == 0 && m.getName().equals( "finalize" ) );
		}
	};

	protected static final Class[] NO_CLASSES = new Class[0];
	private Class persistentClass;
	private String entityName;
	private Class[] interfaces;
	private Method getIdentifierMethod;
	private Method setIdentifierMethod;
	private CompositeType componentIdType;
	private Class proxyClass;
	private boolean overridesEquals;

	@Override
	public void postInstantiate(
			final String entityName,
			final Class persistentClass,
			final Set<Class> interfaces,
			final Method getIdentifierMethod,
			final Method setIdentifierMethod,
			CompositeType componentIdType) throws HibernateException {
		this.entityName = entityName;
		this.persistentClass = persistentClass;
		this.interfaces = interfaces.toArray(NO_CLASSES);
		this.getIdentifierMethod = getIdentifierMethod;
		this.setIdentifierMethod = setIdentifierMethod;
		this.componentIdType = componentIdType;
		this.proxyClass = getProxyClass();
		this.overridesEquals = ReflectHelper.overridesEquals( persistentClass );
	}

	@Override
	public HibernateProxy getProxy(
			Serializable id,
			SessionImplementor session) throws HibernateException {
		final HibernateProxy proxy;
		try {
			proxy = ( HibernateProxy ) proxyClass.newInstance();
		}
		catch ( Exception e ) {
			throw new HibernateException(
					"Javassist Enhancement failed: "
					+ persistentClass.getName(), e
			);
		}

		final BasicLazyInitializer initializer = new JavassistLazyInitializer(
				entityName,
				persistentClass,
				interfaces, id,
				getIdentifierMethod,
				setIdentifierMethod,
				componentIdType,
				session,
				overridesEquals
		);
		final MethodHandler methodHandler = new JavassistMethodHandler( true, initializer );
		( (ProxyObject) proxy ).setHandler( methodHandler );
		return proxy;
	}

	private Class getProxyClass() throws HibernateException {
		// note: interfaces is assumed to already contain HibernateProxy.class
		try {
			javassist.util.proxy.ProxyFactory factory = new javassist.util.proxy.ProxyFactory();
			factory.setSuperclass( interfaces.length == 1 ? persistentClass : null );
			factory.setInterfaces( interfaces );
			factory.setFilter( FINALIZE_FILTER );
			return factory.createClass();
		}
		catch ( Exception e ) {
			LOG.error( LOG.javassistEnhancementFailed( persistentClass.getName() ), e );
			throw new HibernateException(LOG.javassistEnhancementFailed( persistentClass.getName() ), e );
		}
	}

}
