/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;


import java.util.function.Supplier;

import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.metamodel.spi.ManagedTypeRepresentationResolver;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.usertype.CompositeUserType;

/**
 * @author Steve Ebersole
 */
public class ManagedTypeRepresentationResolverStandard implements ManagedTypeRepresentationResolver {
	/**
	 * Singleton access
	 */
	public static final ManagedTypeRepresentationResolverStandard INSTANCE = new ManagedTypeRepresentationResolverStandard();

	@Override
	public EntityRepresentationStrategy resolveStrategy(
			PersistentClass bootDescriptor,
			EntityPersister runtimeDescriptor,
			RuntimeModelCreationContext creationContext) {
//		RepresentationMode representation = bootDescriptor.getExplicitRepresentationMode();
		RepresentationMode representation = null;
		if ( representation == null ) {
			if ( bootDescriptor.getMappedClass() == null ) {
				representation = RepresentationMode.MAP;
			}
			else {
				representation = RepresentationMode.POJO;
			}
		}

		if ( representation == RepresentationMode.MAP ) {
			return new EntityRepresentationStrategyMap( bootDescriptor, creationContext );
		}
		else {
			// todo (6.0) : fix this
			// 		currently we end up resolving the ReflectionOptimizer from the BytecodeProvider
			//		multiple times per class
			//
			//		instead, resolve ReflectionOptimizer once - here - and pass along to
			//		StandardPojoRepresentationStrategy
			return new EntityRepresentationStrategyPojoStandard( bootDescriptor, runtimeDescriptor, creationContext );
		}
	}

	@Override
	public EmbeddableRepresentationStrategy resolveStrategy(
			Component bootDescriptor,
			Supplier<EmbeddableMappingType> runtimeDescriptorAccess,
			RuntimeModelCreationContext creationContext) {
//		RepresentationMode representation = bootDescriptor.getExplicitRepresentationMode();
		RepresentationMode representation = null;
		if ( representation == null ) {
			if ( bootDescriptor.getComponentClassName() == null ) {
				representation = RepresentationMode.MAP;
			}
			else {
				representation = RepresentationMode.POJO;
			}
		}

		final CompositeUserType<Object> compositeUserType;
		if ( bootDescriptor.getTypeName() != null ) {
			compositeUserType = (CompositeUserType<Object>) creationContext.getBootstrapContext()
					.getServiceRegistry()
					.getService( ManagedBeanRegistry.class )
					.getBean(
							creationContext.getBootstrapContext()
									.getClassLoaderAccess()
									.classForName( bootDescriptor.getTypeName() )
					)
					.getBeanInstance();
		}
		else {
			compositeUserType = null;
		}
		final EmbeddableInstantiator customInstantiator;
		if ( bootDescriptor.getCustomInstantiator() != null ) {
			final Class<? extends EmbeddableInstantiator> customInstantiatorImpl = bootDescriptor.getCustomInstantiator();
			customInstantiator = creationContext.getBootstrapContext()
					.getServiceRegistry()
					.getService( ManagedBeanRegistry.class )
					.getBean( customInstantiatorImpl )
					.getBeanInstance();
		}
		else if ( compositeUserType != null ) {
			customInstantiator = new EmbeddableCompositeUserTypeInstantiator( compositeUserType );
		}
		else {
			customInstantiator = null;
		}

		if ( representation == RepresentationMode.MAP ) {
			return new EmbeddableRepresentationStrategyMap(
					bootDescriptor,
					runtimeDescriptorAccess,
					customInstantiator,
					creationContext
			);
		}
		else {
			// todo (6.0) : fix this
			// 		currently we end up resolving the ReflectionOptimizer from the BytecodeProvider
			//		multiple times per class
			//
			//		instead, resolve ReflectionOptimizer once - here - and pass along to
			//		StandardPojoRepresentationStrategy
			return new EmbeddableRepresentationStrategyPojo(
					bootDescriptor,
					runtimeDescriptorAccess,
					customInstantiator,
					compositeUserType,
					creationContext
			);
		}
	}
}
