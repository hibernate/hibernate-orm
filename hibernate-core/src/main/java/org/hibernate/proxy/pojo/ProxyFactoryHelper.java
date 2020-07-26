/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy.pojo;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Subclass;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.tuple.entity.PojoEntityTuplizer;

/**
 * Most of this code was originally an internal detail of {@link PojoEntityTuplizer},
 * then extracted to make it easier for integrators to initialize a custom
 * {@link org.hibernate.proxy.ProxyFactory}.
 */
public final class ProxyFactoryHelper {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( ProxyFactoryHelper.class );

	private ProxyFactoryHelper() {
		//not meant to be instantiated
	}

	public static Set<Class> extractProxyInterfaces(final PersistentClass persistentClass, final String entityName) {
		/*
		 * We need to preserve the order of the interfaces they were put into the set, since javassist will choose the
		 * first one's class-loader to construct the proxy class with. This is also the reason why HibernateProxy.class
		 * should be the last one in the order (on JBossAS7 its class-loader will be org.hibernate module's class-
		 * loader, which will not see the classes inside deployed apps.  See HHH-3078
		 */
		final Set<Class> proxyInterfaces = new java.util.LinkedHashSet<Class>();
		final Class mappedClass = persistentClass.getMappedClass();
		final Class proxyInterface = persistentClass.getProxyInterface();

		if ( proxyInterface != null && !mappedClass.equals( proxyInterface ) ) {
			if ( !proxyInterface.isInterface() ) {
				throw new MappingException(
						"proxy must be either an interface, or the class itself: " + entityName
				);
			}
			proxyInterfaces.add( proxyInterface );
		}

		if ( mappedClass.isInterface() ) {
			proxyInterfaces.add( mappedClass );
		}

		Iterator<Subclass> subclasses = persistentClass.getSubclassIterator();
		while ( subclasses.hasNext() ) {
			final Subclass subclass = subclasses.next();
			final Class subclassProxy = subclass.getProxyInterface();
			final Class subclassClass = subclass.getMappedClass();
			if ( subclassProxy != null && !subclassClass.equals( subclassProxy ) ) {
				if ( !subclassProxy.isInterface() ) {
					throw new MappingException(
							"proxy must be either an interface, or the class itself: " + subclass.getEntityName()
					);
				}
				proxyInterfaces.add( subclassProxy );
			}
		}

		proxyInterfaces.add( HibernateProxy.class );
		return proxyInterfaces;
	}

	public static void validateProxyability(final PersistentClass persistentClass) {
		Iterator properties = persistentClass.getPropertyIterator();
		Class clazz = persistentClass.getMappedClass();
		while ( properties.hasNext() ) {
			Property property = (Property) properties.next();
			validateGetterSetterMethodProxyability( "Getter", property.getGetter( clazz ).getMethod() );
			validateGetterSetterMethodProxyability( "Setter", property.getSetter( clazz ).getMethod() );
		}
	}

	public static void validateGetterSetterMethodProxyability(String getterOrSetter, Method method ) {
		if ( method != null && Modifier.isFinal( method.getModifiers() ) ) {
			throw new HibernateException(
					String.format(
							"%s methods of lazy classes cannot be final: %s#%s",
							getterOrSetter,
							method.getDeclaringClass().getName(),
							method.getName()
					)
			);
		}
	}

	public static Method extractProxySetIdentifierMethod(final Setter idSetter, final Class proxyInterface) {
		Method idSetterMethod = idSetter == null ? null : idSetter.getMethod();

		Method proxySetIdentifierMethod = idSetterMethod == null || proxyInterface == null ?
				null :
				ReflectHelper.getMethod( proxyInterface, idSetterMethod );
		return proxySetIdentifierMethod;
	}

	public static Method extractProxyGetIdentifierMethod(final Getter idGetter, final Class proxyInterface) {
		Method idGetterMethod = idGetter == null ? null : idGetter.getMethod();

		Method proxyGetIdentifierMethod = idGetterMethod == null || proxyInterface == null ?
				null :
				ReflectHelper.getMethod( proxyInterface, idGetterMethod );
		return proxyGetIdentifierMethod;
	}
}
