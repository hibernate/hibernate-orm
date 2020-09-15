/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.entity;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.proxy.pojo.ProxyFactoryHelper;
import org.hibernate.type.CompositeType;

/**
 * @author Andrea Boriero
 */
public class PojoPoxyBuilder implements ProxyBuilder {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( PojoPoxyBuilder.class );

	@Override
	public ProxyFactory buildProxyFactory(
			MappedSuperclass mappedSuperclass,
			Getter idGetter,
			Setter idSetter,
			SessionFactoryImplementor sessionFactory) {
		final String entityName = mappedSuperclass.getMappedClass().getName();
		final Class mappedClass = mappedSuperclass.getMappedClass();
		final Class proxyInterface = mappedSuperclass.getProxyInterface();
		final Iterator declaredPropertyIterator = mappedSuperclass.getDeclaredPropertyIterator();
		final PersistentClass superPersistentClass = mappedSuperclass.getSuperPersistentClass();

		return buildProxyFactory(
				superPersistentClass,
				idSetter,
				idGetter,
				entityName,
				mappedClass,
				proxyInterface,
				declaredPropertyIterator,
				sessionFactory
		);
	}

	@Override
	public ProxyFactory buildProxyFactory(
			PersistentClass persistentClass,
			String entityName,
			Getter idGetter,
			Setter idSetter,
			SessionFactoryImplementor sessionFactory) {
		final Class mappedClass = persistentClass.getMappedClass();
		final Class proxyInterface = persistentClass.getProxyInterface();
		final Iterator propertyIterator = persistentClass.getPropertyIterator();

		return buildProxyFactory(
				persistentClass,
				idSetter,
				idGetter,
				entityName,
				mappedClass,
				proxyInterface,
				propertyIterator,
				sessionFactory
		);
	}

	private ProxyFactory buildProxyFactory(
			PersistentClass persistentClass,
			Setter idSetter,
			Getter idGetter,
			String entityName,
			Class mappedClass,
			Class proxyInterface,
			Iterator propertyIterator,
			SessionFactoryImplementor sessionFactory) {
		final Set<Class> proxyInterfaces = ProxyFactoryHelper.extractProxyInterfaces(
				persistentClass,
				entityName
		);

		Method proxyGetIdentifierMethod = ProxyFactoryHelper.extractProxyGetIdentifierMethod(
				idGetter,
				proxyInterface
		);
		Method proxySetIdentifierMethod = ProxyFactoryHelper.extractProxySetIdentifierMethod(
				idSetter,
				proxyInterface
		);

		ProxyFactoryFactory proxyFactory = sessionFactory.getServiceRegistry().getService( ProxyFactoryFactory.class );
		ProxyFactory pf = proxyFactory.buildProxyFactory( sessionFactory );
		try {

			ProxyFactoryHelper.validateGetterSetterMethodProxyability( "Getter", proxyGetIdentifierMethod );
			ProxyFactoryHelper.validateGetterSetterMethodProxyability( "Setter", proxySetIdentifierMethod );

			ProxyFactoryHelper.validateProxyability( propertyIterator, mappedClass );

			pf.postInstantiate(
					entityName,
					mappedClass,
					proxyInterfaces,
					proxyGetIdentifierMethod,
					proxySetIdentifierMethod,
					persistentClass.hasEmbeddedIdentifier() ?
							(CompositeType) persistentClass.getIdentifier().getType() :
							null
			);
		}
		catch (HibernateException he) {
			LOG.unableToCreateProxyFactory( entityName, he );
			pf = null;
		}
		return pf;
	}

}
