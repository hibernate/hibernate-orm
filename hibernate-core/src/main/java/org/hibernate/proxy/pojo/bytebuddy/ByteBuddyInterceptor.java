/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy.pojo.bytebuddy;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.proxy.ProxyConfiguration;
import org.hibernate.proxy.pojo.BasicLazyInitializer;
import org.hibernate.type.CompositeType;

import static org.hibernate.internal.CoreLogging.messageLogger;

public class ByteBuddyInterceptor extends BasicLazyInitializer implements ProxyConfiguration.Interceptor {
	private static final CoreMessageLogger LOG = messageLogger( ByteBuddyInterceptor.class );

	private final Class[] interfaces;

	public ByteBuddyInterceptor(
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

	@Override
	public Object intercept(Object proxy, Method thisMethod, Object[] args) throws Throwable {
		Object result = this.invoke( thisMethod, args, proxy );
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
			catch (InvocationTargetException ite) {
				throw ite.getTargetException();
			}
		}
		else {
			return result;
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
