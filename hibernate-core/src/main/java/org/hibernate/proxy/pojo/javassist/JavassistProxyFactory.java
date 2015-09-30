/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy.pojo.javassist;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Set;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.Proxy;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.CompositeType;

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * A {@link ProxyFactory} implementation for producing Javassist-based proxies.
 *
 * @author Muga Nishizawa
 */
public class JavassistProxyFactory implements ProxyFactory, Serializable {
	private static final CoreMessageLogger LOG = messageLogger( JavassistProxyFactory.class );

	private static final MethodFilter FINALIZE_FILTER = new MethodFilter() {
		public boolean isHandled(Method m) {
			// skip finalize methods
			return !( m.getParameterTypes().length == 0 && m.getName().equals( "finalize" ) );
		}
	};

	private Class persistentClass;
	private String entityName;
	private Class[] interfaces;
	private Method getIdentifierMethod;
	private Method setIdentifierMethod;
	private CompositeType componentIdType;
	private boolean overridesEquals;

	private Class proxyClass;

	public JavassistProxyFactory() {
	}

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
		this.interfaces = toArray( interfaces );
		this.getIdentifierMethod = getIdentifierMethod;
		this.setIdentifierMethod = setIdentifierMethod;
		this.componentIdType = componentIdType;
		this.overridesEquals = ReflectHelper.overridesEquals( persistentClass );

		this.proxyClass = buildJavassistProxyFactory().createClass();
	}

	private Class[] toArray(Set<Class> interfaces) {
		if ( interfaces == null ) {
			return ArrayHelper.EMPTY_CLASS_ARRAY;
		}

		return interfaces.toArray( new Class[interfaces.size()] );
	}

	private javassist.util.proxy.ProxyFactory buildJavassistProxyFactory() {
		return buildJavassistProxyFactory(
				persistentClass,
				interfaces
		);
	}

	public static javassist.util.proxy.ProxyFactory buildJavassistProxyFactory(
			final Class persistentClass,
			final Class[] interfaces) {
		javassist.util.proxy.ProxyFactory factory = new javassist.util.proxy.ProxyFactory() {
			@Override
			protected ClassLoader getClassLoader() {
				return persistentClass.getClassLoader();
			}
		};
		factory.setSuperclass( interfaces.length == 1 ? persistentClass : null );
		factory.setInterfaces( interfaces );
		factory.setFilter( FINALIZE_FILTER );
		return factory;
	}

	@Override
	public HibernateProxy getProxy(
			Serializable id,
			SessionImplementor session) throws HibernateException {
		final JavassistLazyInitializer initializer = new JavassistLazyInitializer(
				entityName,
				persistentClass,
				interfaces,
				id,
				getIdentifierMethod,
				setIdentifierMethod,
				componentIdType,
				session,
				overridesEquals
		);

		try {
			final HibernateProxy proxy = (HibernateProxy) proxyClass.newInstance();
			( (Proxy) proxy ).setHandler( initializer );
			initializer.constructed();

			return proxy;
		}
		catch (Throwable t) {
			LOG.error( LOG.javassistEnhancementFailed( entityName ), t );
			throw new HibernateException( LOG.javassistEnhancementFailed( entityName ), t );
		}
	}

	public static HibernateProxy deserializeProxy(SerializableProxy serializableProxy) {
		final JavassistLazyInitializer initializer = new JavassistLazyInitializer(
				serializableProxy.getEntityName(),
				serializableProxy.getPersistentClass(),
				serializableProxy.getInterfaces(),
				serializableProxy.getId(),
				resolveIdGetterMethod( serializableProxy ),
				resolveIdSetterMethod( serializableProxy ),
				serializableProxy.getComponentIdType(),
				null,
				ReflectHelper.overridesEquals( serializableProxy.getPersistentClass() )
		);

		final javassist.util.proxy.ProxyFactory factory = buildJavassistProxyFactory(
				serializableProxy.getPersistentClass(),
				serializableProxy.getInterfaces()
		);

		// note: interface is assumed to already contain HibernateProxy.class
		try {
			final Class proxyClass = factory.createClass();
			final HibernateProxy proxy = ( HibernateProxy ) proxyClass.newInstance();
			( (Proxy) proxy ).setHandler( initializer );
			initializer.constructed();
			return proxy;
		}
		catch ( Throwable t ) {
			final String message = LOG.javassistEnhancementFailed( serializableProxy.getEntityName() );
			LOG.error( message, t );
			throw new HibernateException( message, t );
		}
	}

	@SuppressWarnings("unchecked")
	private static Method resolveIdGetterMethod(SerializableProxy serializableProxy) {
		if ( serializableProxy.getIdentifierGetterMethodName() == null ) {
			return null;
		}

		try {
			return serializableProxy.getIdentifierGetterMethodClass().getDeclaredMethod( serializableProxy.getIdentifierGetterMethodName() );
		}
		catch (NoSuchMethodException e) {
			throw new HibernateException(
					String.format(
							Locale.ENGLISH,
							"Unable to deserialize proxy [%s, %s]; could not locate id getter method [%s] on entity class [%s]",
							serializableProxy.getEntityName(),
							serializableProxy.getId(),
							serializableProxy.getIdentifierGetterMethodName(),
							serializableProxy.getIdentifierGetterMethodClass()
					)
			);
		}
	}

	@SuppressWarnings("unchecked")
	private static Method resolveIdSetterMethod(SerializableProxy serializableProxy) {
		if ( serializableProxy.getIdentifierSetterMethodName() == null ) {
			return null;
		}

		try {
			return serializableProxy.getIdentifierSetterMethodClass().getDeclaredMethod(
					serializableProxy.getIdentifierSetterMethodName(),
					serializableProxy.getIdentifierSetterMethodParams()
			);
		}
		catch (NoSuchMethodException e) {
			throw new HibernateException(
					String.format(
							Locale.ENGLISH,
							"Unable to deserialize proxy [%s, %s]; could not locate id setter method [%s] on entity class [%s]",
							serializableProxy.getEntityName(),
							serializableProxy.getId(),
							serializableProxy.getIdentifierSetterMethodName(),
							serializableProxy.getIdentifierSetterMethodClass()
					)
			);
		}
	}
}
