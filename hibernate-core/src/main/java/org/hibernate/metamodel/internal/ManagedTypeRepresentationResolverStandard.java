/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;


import java.util.function.Supplier;

import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.metamodel.spi.ManagedTypeRepresentationResolver;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.usertype.CompositeUserType;

import static org.hibernate.internal.util.ReflectHelper.isRecord;

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
		if ( bootDescriptor.getMappedClass() == null ) { // i.e. RepresentationMode.MAP;
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

		final var compositeUserType = getCompositeUserType( bootDescriptor, creationContext );
		final var customInstantiator =
				getCustomInstantiator( bootDescriptor, creationContext, compositeUserType );

		if ( bootDescriptor.getComponentClassName() == null ) { // i.e. RepresentationMode.MAP;
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
			//noinspection unchecked
			return new EmbeddableRepresentationStrategyPojo(
					bootDescriptor,
					runtimeDescriptorAccess,
					customInstantiator,
					(CompositeUserType<Object>) compositeUserType,
					creationContext
			);
		}
	}

	private static CompositeUserType<?> getCompositeUserType(
			Component bootDescriptor, RuntimeModelCreationContext creationContext) {
		if ( bootDescriptor.getTypeName() != null ) {
			return beanInstance( creationContext,
					creationContext.getBootstrapContext().getClassLoaderAccess()
							.classForName( bootDescriptor.getTypeName() ) );
		}
		else {
			return null;
		}
	}

	private static EmbeddableInstantiator getCustomInstantiator(
			Component bootDescriptor, RuntimeModelCreationContext creationContext, CompositeUserType<?> compositeUserType) {
		if ( bootDescriptor.getCustomInstantiator() != null ) {
			return beanInstance( creationContext, bootDescriptor.getCustomInstantiator() );
		}
		else if ( compositeUserType != null ) {
			//noinspection unchecked,rawtypes
			return new EmbeddableCompositeUserTypeInstantiator( (CompositeUserType) compositeUserType );
		}
		else if ( bootDescriptor.getComponentClassName() != null
				&& isRecord( bootDescriptor.getComponentClass() ) ) {
			if ( bootDescriptor.sortProperties() == null ) {
				return new EmbeddableInstantiatorRecordStandard( bootDescriptor.getComponentClass() );
			}
			else {
				return EmbeddableInstantiatorRecordIndirecting.of(
						bootDescriptor.getComponentClass(),
						bootDescriptor.getPropertyNames()
				);
			}
		}
		else if ( bootDescriptor.getInstantiator() != null ) {
			bootDescriptor.sortProperties();
			return EmbeddableInstantiatorPojoIndirecting.of(
					bootDescriptor.getPropertyNames(),
					bootDescriptor.getInstantiator(),
					bootDescriptor.getInstantiatorPropertyNames()
			);
		}
		else {
			return null;
		}
	}

	private static <T> T beanInstance(RuntimeModelCreationContext creationContext, Class<T> userTypeClass) {
		return creationContext.getBootModel().getMetadataBuildingOptions().isAllowExtensionsInCdi()
				? getBeanInstance( creationContext, userTypeClass )
				: FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( userTypeClass );
	}

	private static <T> T getBeanInstance(RuntimeModelCreationContext creationContext, Class<T> userTypeClass) {
		return creationContext.getBootstrapContext().getManagedBeanRegistry()
				.getBean( userTypeClass )
				.getBeanInstance();
	}
}
