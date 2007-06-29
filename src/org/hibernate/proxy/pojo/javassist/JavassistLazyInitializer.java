package org.hibernate.proxy.pojo.javassist;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.proxy.pojo.BasicLazyInitializer;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.util.ReflectHelper;

/**
 * A Javassist-based lazy initializer proxy.
 *
 * @author Muga Nishizawa
 */
public class JavassistLazyInitializer extends BasicLazyInitializer implements MethodHandler {

	private static final MethodFilter FINALIZE_FILTER = new MethodFilter() {
		public boolean isHandled(Method m) {
			// skip finalize methods
			return !( m.getParameterTypes().length == 0 && m.getName().equals( "finalize" ) );
		}
	};

	private Class[] interfaces;
	private boolean constructed = false;

	private JavassistLazyInitializer(
			final String entityName,
	        final Class persistentClass,
	        final Class[] interfaces,
	        final Serializable id,
	        final Method getIdentifierMethod,
	        final Method setIdentifierMethod,
	        final AbstractComponentType componentIdType,
	        final SessionImplementor session) {
		super( entityName, persistentClass, id, getIdentifierMethod, setIdentifierMethod, componentIdType, session );
		this.interfaces = interfaces;
	}

	public static HibernateProxy getProxy(
			final String entityName,
	        final Class persistentClass,
	        final Class[] interfaces,
	        final Method getIdentifierMethod,
	        final Method setIdentifierMethod,
	        AbstractComponentType componentIdType,
	        final Serializable id,
	        final SessionImplementor session) throws HibernateException {
		// note: interface is assumed to already contain HibernateProxy.class
		try {
			final JavassistLazyInitializer instance = new JavassistLazyInitializer(
					entityName,
			        persistentClass,
			        interfaces,
			        id,
			        getIdentifierMethod,
			        setIdentifierMethod,
			        componentIdType,
			        session
			);
			ProxyFactory factory = new ProxyFactory();
			factory.setSuperclass( interfaces.length == 1 ? persistentClass : null );
			factory.setInterfaces( interfaces );
			factory.setFilter( FINALIZE_FILTER );
			Class cl = factory.createClass();
			final HibernateProxy proxy = ( HibernateProxy ) cl.newInstance();
			( ( ProxyObject ) proxy ).setHandler( instance );
			instance.constructed = true;
			return proxy;
		}
		catch ( Throwable t ) {
			LogFactory.getLog( BasicLazyInitializer.class ).error(
					"Javassist Enhancement failed: " + entityName, t
			);
			throw new HibernateException(
					"Javassist Enhancement failed: "
					+ entityName, t
			);
		}
	}

	public static HibernateProxy getProxy(
			final Class factory,
	        final String entityName,
	        final Class persistentClass,
	        final Class[] interfaces,
	        final Method getIdentifierMethod,
	        final Method setIdentifierMethod,
	        final AbstractComponentType componentIdType,
	        final Serializable id,
	        final SessionImplementor session) throws HibernateException {

		final JavassistLazyInitializer instance = new JavassistLazyInitializer(
				entityName,
		        persistentClass,
		        interfaces, id,
		        getIdentifierMethod,
		        setIdentifierMethod,
		        componentIdType,
		        session
		);

		final HibernateProxy proxy;
		try {
			proxy = ( HibernateProxy ) factory.newInstance();
		}
		catch ( Exception e ) {
			throw new HibernateException(
					"Javassist Enhancement failed: "
					+ persistentClass.getName(), e
			);
		}
		( ( ProxyObject ) proxy ).setHandler( instance );
		instance.constructed = true;
		return proxy;
	}

	public static Class getProxyFactory(
			Class persistentClass,
	        Class[] interfaces) throws HibernateException {
		// note: interfaces is assumed to already contain HibernateProxy.class

		try {
			ProxyFactory factory = new ProxyFactory();
			factory.setSuperclass( interfaces.length == 1 ? persistentClass : null );
			factory.setInterfaces( interfaces );
			factory.setFilter( FINALIZE_FILTER );
			return factory.createClass();
		}
		catch ( Throwable t ) {
			LogFactory.getLog( BasicLazyInitializer.class ).error(
					"Javassist Enhancement failed: "
					+ persistentClass.getName(), t
			);
			throw new HibernateException(
					"Javassist Enhancement failed: "
					+ persistentClass.getName(), t
			);
		}
	}

	public Object invoke(
			final Object proxy,
			final Method thisMethod,
			final Method proceed,
			final Object[] args) throws Throwable {
		if ( this.constructed ) {
			Object result;
			try {
				result = this.invoke( thisMethod, args, proxy );
			}
			catch ( Throwable t ) {
				throw new Exception( t.getCause() );
			}
			if ( result == INVOKE_IMPLEMENTATION ) {
				Object target = getImplementation();
				final Object returnValue;
				try {
                    if ( ReflectHelper.isPublic( persistentClass, thisMethod ) ) {
						if ( ! thisMethod.getDeclaringClass().isInstance( target ) ) {
                    		throw new ClassCastException( target.getClass().getName() );
						}
                    	returnValue = thisMethod.invoke( target, args );
                    }
                    else {
                    	if ( !thisMethod.isAccessible() ) {
                    		thisMethod.setAccessible( true );
                    	}
                    	returnValue = thisMethod.invoke( target, args );
                    }
                    return returnValue == target ? proxy : returnValue;
                }
				catch ( InvocationTargetException ite ) {
                    throw ite.getTargetException();
                }
			}
			else {
				return result;
			}
		}
		else {
			// while constructor is running
			if ( thisMethod.getName().equals( "getHibernateLazyInitializer" ) ) {
				return this;
			}
			else {
				return proceed.invoke( proxy, args );
			}
		}
	}

	protected Object serializableProxy() {
		return new SerializableProxy(
				getEntityName(),
		        persistentClass,
		        interfaces,
		        getIdentifier(),
		        getIdentifierMethod,
		        setIdentifierMethod,
		        componentIdType
		);
	}
}
