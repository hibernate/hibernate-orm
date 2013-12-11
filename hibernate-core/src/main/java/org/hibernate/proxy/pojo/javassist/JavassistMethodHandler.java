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

import javassist.util.proxy.MethodHandler;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.proxy.pojo.BasicLazyInitializer;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Javassist method handler for Javassist-proxy object
 *
 * @author Aleksander Dukhno
 */
class JavassistMethodHandler implements MethodHandler {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( JavassistMethodHandler.class );
	protected static final Object INVOKE_IMPLEMENTATION = new MarkerObject( "INVOKE_IMPLEMENTATION" );

	private boolean constructed;
	private BasicLazyInitializer lazyInitializer;

	public JavassistMethodHandler( boolean constructed, BasicLazyInitializer lazyInitializer ) {
		this.lazyInitializer = lazyInitializer;
		this.constructed = constructed;
	}

	boolean isConstructed() {
		return constructed;
	}

	@Override
	public Object invoke(
			final Object proxy,
			final Method thisMethod,
			final Method proceed,
			final Object[] args )
			throws Throwable {
		if( isConstructed() ) {
			Object result;
			try {
				result = invoke( thisMethod, args, proxy );
			}
			catch( Throwable t ) {
				throw new Exception( t.getCause() );
			}
			if( result == INVOKE_IMPLEMENTATION ) {
				Object target = lazyInitializer.getImplementation();
				final Object returnValue;
				try {
					if( ReflectHelper.isPublic( lazyInitializer.getPersistentClass(), thisMethod ) ) {
						if( !thisMethod.getDeclaringClass().isInstance( target ) ) {
							throw new ClassCastException( target.getClass().getName() );
						}
						returnValue = thisMethod.invoke( target, args );
					}
					else {
						if( !thisMethod.isAccessible() ) {
							thisMethod.setAccessible( true );
						}
						returnValue = thisMethod.invoke( target, args );
					}

					if( returnValue == target ) {
						if( returnValue.getClass().isInstance( proxy ) ) {
							return proxy;
						}
						else {
							LOG.narrowingProxy( returnValue.getClass() );
						}
					}
					return returnValue;
				}
				catch( InvocationTargetException ite ) {
					throw ite.getTargetException();
				}
			}
			else {
				return result;
			}
		}
		else {
			// while constructor is running
			if( thisMethod.getName().equals( "getHibernateLazyInitializer" ) ) {
				return lazyInitializer;
			}
			else {
				return proceed.invoke( proxy, args );
			}
		}
	}

	private Object invoke( Method method, Object[] args, Object proxy )
			throws Throwable {
		String methodName = method.getName();
		int params = args.length;

		if( params == 0 ) {
			if( "writeReplace".equals( methodName ) ) {
				return getReplacement();
			}
			else if( !lazyInitializer.isOverridesEquals()
					 && "hashCode".equals( methodName ) ) {
				return System.identityHashCode( proxy );
			}
			else if( lazyInitializer.isUninitialized()
					 && method.equals( lazyInitializer.getGetIdentifierMethod() ) ) {
				return lazyInitializer.getIdentifier();
			}
			else if( "getHibernateLazyInitializer".equals( methodName ) ) {
				return lazyInitializer;
			}
		}
		else if( params == 1 ) {
			if( !lazyInitializer.isOverridesEquals() && "equals".equals( methodName ) ) {
				return args[ 0 ] == proxy;
			}
			else if( method.equals( lazyInitializer.getSetIdentifierMethod() ) ) {
				lazyInitializer.initialize();
				lazyInitializer.setIdentifier( (Serializable) args[ 0 ] );
				return INVOKE_IMPLEMENTATION;
			}
		}

		//if it is a property of an embedded component, invoke on the "identifier"
		if( lazyInitializer.getComponentIdType() != null && lazyInitializer.getComponentIdType().isMethodOf( method ) ) {
			return method.invoke( lazyInitializer.getIdentifier(), args );
		}

		// otherwise:
		return INVOKE_IMPLEMENTATION;

	}

	private Object getReplacement() {
		final SessionImplementor session = lazyInitializer.getSession();
		if( lazyInitializer.isUninitialized() && session != null && session.isOpen() ) {
			final EntityKey key = session.generateEntityKey(
					lazyInitializer.getIdentifier(),
					session.getFactory().getEntityPersister( lazyInitializer.getEntityName() ) );
			final Object entity = session.getPersistenceContext().getEntity( key );
			if( entity != null ) {
				lazyInitializer.setImplementation( entity );
			}
		}
		if( lazyInitializer.isUninitialized() ) {
			if( lazyInitializer.isReplacementNull() ) {
				lazyInitializer.generateReplacement();
			}
			return lazyInitializer.getReplacement();
		}
		else {
			return lazyInitializer.getTarget();
		}
	}

}
