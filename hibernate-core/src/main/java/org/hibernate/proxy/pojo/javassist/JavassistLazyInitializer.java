/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy.pojo.javassist;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javassist.util.proxy.MethodHandler;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.proxy.pojo.BasicLazyInitializer;
import org.hibernate.type.CompositeType;

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * A Javassist-based lazy initializer proxy.
 *
 * @author Muga Nishizawa
 */
public class JavassistLazyInitializer extends BasicLazyInitializer implements MethodHandler {
	private static final CoreMessageLogger LOG = messageLogger( JavassistLazyInitializer.class );

	private final Class[] interfaces;

	private boolean constructed;

	public JavassistLazyInitializer(
			String entityName,
			Class persistentClass,
			Class[] interfaces,
			Serializable id,
			Method getIdentifierMethod,
			Method setIdentifierMethod,
			CompositeType componentIdType,
			SharedSessionContractImplementor session,
			boolean overridesEquals) {
		super( entityName, persistentClass, id, getIdentifierMethod, setIdentifierMethod, componentIdType, session, overridesEquals );
		this.interfaces = interfaces;
	}

	protected void constructed() {
		constructed = true;
	}

	@Override
	public Object invoke(
			final Object proxy,
			final Method thisMethod,
			final Method proceed,
			final Object[] args) throws Throwable {
		if ( this.constructed ) {
			// HHH-10922 - Internal calls to bytecode enhanced methods cause proxy to be initialized
			if ( thisMethod.getName().startsWith( "$$_hibernate_" ) ) {
				return proceed.invoke( proxy, args );
			}

			Object result;
			try {
				result = this.invoke( thisMethod, args, proxy );
			}
			catch ( Throwable t ) {
				throw t instanceof RuntimeException ? t : new Exception( t.getCause() );
			}
			if ( result == INVOKE_IMPLEMENTATION ) {
				Object target = getImplementation();
				final Object returnValue;
				try {
					if ( ReflectHelper.isPublic( persistentClass, thisMethod ) ) {
						if ( !thisMethod.getDeclaringClass().isInstance( target ) ) {
							throw new ClassCastException(
									target.getClass().getName()
									+ " incompatible with "
									+ thisMethod.getDeclaringClass().getName()
							);
						}
						returnValue = thisMethod.invoke( target, args );
					}
					else {
						thisMethod.setAccessible( true );
						returnValue = thisMethod.invoke( target, args );
					}
					
					if ( returnValue == target ) {
						if ( returnValue.getClass().isInstance( proxy ) ) {
							return proxy;
						}
						else {
							LOG.narrowingProxy( returnValue.getClass() );
						}
					}
					return returnValue;
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

	@Override
	protected Object serializableProxy() {
		return new SerializableProxy(
				getEntityName(),
				persistentClass,
				interfaces,
				getIdentifier(),
				( isReadOnlySettingAvailable() ? Boolean.valueOf( isReadOnly() ) : isReadOnlyBeforeAttachedToSession() ),
				getSessionFactoryUuid(),
				isAllowLoadOutsideTransaction(),
				getIdentifierMethod,
				setIdentifierMethod,
				componentIdType
		);
	}
}
