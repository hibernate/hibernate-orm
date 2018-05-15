/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.mode;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractEntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.InheritanceCapable;
import org.hibernate.metamodel.model.domain.spi.ProxyFactoryInstantiator;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;

/**
 * @author Chris Cranford
 */
public class StandardPojoProxyFactoryInstantiator<J> implements ProxyFactoryInstantiator<J> {
	public static final StandardPojoProxyFactoryInstantiator INSTANCE = new StandardPojoProxyFactoryInstantiator();

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( StandardPojoProxyFactoryInstantiator.class );

	// todo (6.0) - We may want to merge this into StandardPojoRepresentationStrategy?

	@Override
	public ProxyFactory instantiate(
			EntityTypeDescriptor<J> runtimeDescriptor,
			RuntimeModelCreationContext creationContext) {
		final EntityIdentifier identifierDescriptor = runtimeDescriptor.getHierarchy().getIdentifierDescriptor();

		// todo (6.0) - can refactor some of this out with the bytecode provider
		PropertyAccess propertyAccess = identifierDescriptor.asAttribute( identifierDescriptor.getJavaType() )
				.getPropertyAccess();
		final Getter idGetter = propertyAccess.getGetter();
		final Setter idSetter = propertyAccess.getSetter();
		final Set<Class> proxyInterfaces = new java.util.LinkedHashSet<>();

		final Class mappedClass = runtimeDescriptor.getJavaTypeDescriptor().getJavaType();
		addProxyInterfaces( runtimeDescriptor, proxyInterfaces );

		if ( mappedClass.isInterface() ) {
			proxyInterfaces.add( mappedClass );
		}

		Collection<InheritanceCapable<? extends J>> subclassTypes = runtimeDescriptor.getSubclassTypes();
		subclassTypes.forEach(
				inheritanceCapable -> {
					if ( inheritanceCapable instanceof AbstractEntityTypeDescriptor ) {
						addProxyInterfaces( runtimeDescriptor, proxyInterfaces );
					}
				}
		);

		proxyInterfaces.add( HibernateProxy.class );
		runtimeDescriptor.visitAttributes(
				attribute -> {
					final PropertyAccess attributePropertyAccess = attribute.getPropertyAccess();
					final Method getterMethod = attributePropertyAccess.getGetter().getMethod();
					if ( getterMethod != null && Modifier.isFinal( getterMethod.getModifiers() ) ) {
						LOG.gettersOfLazyClassesCannotBeFinal( runtimeDescriptor.getEntityName(), attribute.getAttributeName() );
					}

					final Method setterMethod = attributePropertyAccess.getSetter().getMethod();
					if ( setterMethod != null && Modifier.isFinal( setterMethod.getModifiers() ) ) {
						LOG.settersOfLazyClassesCannotBeFinal( runtimeDescriptor.getEntityName(), attribute.getAttributeName() );
					}
				}
		);

		final Method idGetterMethod = idGetter == null ? null : idGetter.getMethod();
		final Method idSetterMethod = idSetter == null ? null : idSetter.getMethod();

		final Class proxyInterface = runtimeDescriptor.getConcreteProxyClass();

		final Method proxyGetIdentifierMethod = idGetterMethod == null || proxyInterface == null ?
				null :
				ReflectHelper.getMethod( proxyInterface, idGetterMethod );
		final Method proxySetIdentifierMethod = idSetterMethod == null || proxyInterface == null ?
				null :
				ReflectHelper.getMethod( proxyInterface, idSetterMethod );

		final ProxyFactory pf = buildProxyFactoryInternal( idGetter, idSetter, creationContext );

		try {
			pf.postInstantiate(
					runtimeDescriptor.getEntityName(),
					mappedClass,
					proxyInterfaces,
					proxyGetIdentifierMethod,
					proxySetIdentifierMethod,
					identifierDescriptor instanceof EmbeddedTypeDescriptor
							? (EmbeddedTypeDescriptor) identifierDescriptor
							: null
			);
		}
		catch ( HibernateException he) {
			LOG.unableToCreateProxyFactory( runtimeDescriptor.getEntityName(), he );
		}
		return pf;
	}

	private ProxyFactory buildProxyFactoryInternal(
			Getter idGetter,
			Setter idSetter,
			RuntimeModelCreationContext creationContext) {
		final SessionFactoryImplementor sessionFactory = creationContext.getSessionFactory();

		return sessionFactory.getSessionFactoryOptions()
				.getBytecodeProvider()
				.getProxyFactoryFactory()
				.buildProxyFactory( sessionFactory );
	}

	private void addProxyInterfaces(EntityTypeDescriptor runtimeDescriptor, Set<Class> proxyInterfaces){
		final Class javaClass = runtimeDescriptor.getJavaTypeDescriptor().getJavaType();
		final Class proxyInterface = runtimeDescriptor.getConcreteProxyClass();
		if ( proxyInterface != null && !javaClass.equals( proxyInterface ) ) {
			if ( !proxyInterface.isInterface() ) {
				throw new MappingException(
						"proxy must be either an interface, or the class itself: " + runtimeDescriptor.getNavigableName()
				);
			}
			proxyInterfaces.add( proxyInterface );
		}
	}

	private StandardPojoProxyFactoryInstantiator() {

	}
}
