/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.spi;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;

import org.hibernate.PropertyAccessException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreMessageLogger;

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * @author Steve Ebersole
 */
public class GetterMethodImpl implements Getter {
	private static final CoreMessageLogger LOG = messageLogger( GetterMethodImpl.class );

	private final Class containerClass;
	private final String propertyName;
	private final Method getterMethod;

	public GetterMethodImpl(Class containerClass, String propertyName, Method getterMethod) {
		this.containerClass = containerClass;
		this.propertyName = propertyName;
		this.getterMethod = getterMethod;
	}

	@Override
	public Object get(Object owner) {
		try {
			return getterMethod.invoke( owner );
		}
		catch (InvocationTargetException ite) {
			throw new PropertyAccessException(
					ite,
					"Exception occurred inside",
					false,
					containerClass,
					propertyName
			);
		}
		catch (IllegalAccessException iae) {
			throw new PropertyAccessException(
					iae,
					"IllegalAccessException occurred while calling",
					false,
					containerClass,
					propertyName
			);
			//cannot occur
		}
		catch (IllegalArgumentException iae) {
			LOG.illegalPropertyGetterArgument( containerClass.getName(), propertyName );
			throw new PropertyAccessException(
					iae,
					"IllegalArgumentException occurred calling",
					false,
					containerClass,
					propertyName
			);
		}
	}

	@Override
	public Object getForInsert(Object owner, Map mergeMap, SessionImplementor session) {
		return get( owner );
	}

	@Override
	public Class getReturnType() {
		return getterMethod.getReturnType();
	}

	@Override
	public Member getMember() {
		return getterMethod;
	}

	@Override
	public String getMethodName() {
		return getterMethod.getName();
	}

	@Override
	public Method getMethod() {
		return getterMethod;
	}

	private Object writeReplace() throws ObjectStreamException {
		return new SerialForm( containerClass, propertyName, getterMethod );
	}

	private static class SerialForm implements Serializable {
		private final Class containerClass;
		private final String propertyName;

		private final Class declaringClass;
		private final String methodName;

		private SerialForm(Class containerClass, String propertyName, Method method) {
			this.containerClass = containerClass;
			this.propertyName = propertyName;
			this.declaringClass = method.getDeclaringClass();
			this.methodName = method.getName();
		}

		private Object readResolve() {
			return new GetterMethodImpl( containerClass, propertyName, resolveMethod() );
		}

		@SuppressWarnings("unchecked")
		private Method resolveMethod() {
			try {
				return declaringClass.getDeclaredMethod( methodName );
			}
			catch (NoSuchMethodException e) {
				throw new PropertyAccessSerializationException(
						"Unable to resolve getter method on deserialization : " + declaringClass.getName() + "#" + methodName
				);
			}
		}
	}
}
