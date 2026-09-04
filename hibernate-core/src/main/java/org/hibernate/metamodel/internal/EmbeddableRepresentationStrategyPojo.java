/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.annotation.Nullable;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.accessor.HibernateAccessorInstantiator;
import org.hibernate.accessor.HibernateAccessorMultiValueReader;
import org.hibernate.accessor.HibernateAccessorMultiValueWriter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessorService;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.internal.CompositeUserTypeJavaTypeWrapper;
import org.hibernate.usertype.CompositeUserType;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.internal.util.ReflectHelper.isAbstractClass;
import static org.hibernate.metamodel.internal.PropertyAccessHelper.propertyAccessStrategy;

/**
 * @author Steve Ebersole
 */
public class EmbeddableRepresentationStrategyPojo implements EmbeddableRepresentationStrategy {
	private final JavaType<?> embeddableJavaType;
	private final PropertyAccess[] propertyAccesses;
	private final Map<String, Integer> attributeNameToPositionMap;

	private final @Nullable HibernateAccessorMultiValueReader multiValueReader;
	private final @Nullable HibernateAccessorMultiValueWriter multiValueWriter;
	private final EmbeddableInstantiator instantiator;
	private final Map<Object, EmbeddableInstantiator> instantiatorsByDiscriminator;
	private final Map<String, EmbeddableInstantiator> instantiatorsByClass;

	public EmbeddableRepresentationStrategyPojo(
			Component bootDescriptor,
			Supplier<EmbeddableMappingType> runtimeDescriptorAccess,
			EmbeddableInstantiator customInstantiator,
			CompositeUserType<Object> compositeUserType,
			RuntimeModelCreationContext creationContext) {
		embeddableJavaType = resolveEmbeddableJavaType( bootDescriptor, compositeUserType, creationContext );

		final int propertySpan = bootDescriptor.getPropertySpan();
		propertyAccesses = new PropertyAccess[propertySpan];
		attributeNameToPositionMap = new HashMap<>( propertySpan );

		final var strategySelector =
				creationContext.getServiceRegistry()
						.getService( StrategySelector.class );

		// We need access to the Class objects, used only during initialization
		final var subclassesByName = getSubclassesByName( bootDescriptor, creationContext );
		boolean foundCustomAccessor = false;
		for ( int i = 0; i < bootDescriptor.getProperties().size(); i++ ) {
			final var property = bootDescriptor.getProperty( i );
			final var embeddableClass = getEmbeddableClass( bootDescriptor, subclassesByName, property );
			propertyAccesses[i] =
					buildPropertyAccess(
							creationContext.getServiceRegistry().requireService( PropertyAccessorService.class ),
							property,
							embeddableClass,
							customInstantiator == null,
							strategySelector
					);
			attributeNameToPositionMap.put( property.getName(), i );

			if ( !property.isBasicPropertyAccessor() ) {
				foundCustomAccessor = true;
			}
		}

		if ( canBuildMultiValueAccessors( bootDescriptor, foundCustomAccessor ) ) {
			final var multiValueAccessors = PropertyAccessHelper.buildMultiValueAccessors(
					creationContext.getServiceRegistry()
							.requireService( PropertyAccessorService.class )
							.hibernateAccessorFactory(),
					bootDescriptor.getComponentClass(),
					Arrays.asList( propertyAccesses )
			);
			multiValueReader = multiValueAccessors.reader();
			multiValueWriter = multiValueAccessors.writer();
		}
		else {
			multiValueReader = null;
			multiValueWriter = null;
		}

		if ( bootDescriptor.isPolymorphic() ) {
			final int size = bootDescriptor.getDiscriminatorValues().size();
			instantiatorsByDiscriminator = new HashMap<>( size );
			instantiatorsByClass = new HashMap<>( size );
			for ( var discriminator : bootDescriptor.getDiscriminatorValues().entrySet() ) {
				final String className = discriminator.getValue();
				final var instantiator = determineInstantiator(
						bootDescriptor,
						castNonNull( subclassesByName ).get( className ),
						runtimeDescriptorAccess,
						creationContext
				);
				instantiatorsByDiscriminator.put( discriminator.getKey(), instantiator );
				instantiatorsByClass.put( className, instantiator );
			}
			instantiator = null;
		}
		else {
			instantiator = customInstantiator != null ?
					customInstantiator :
					determineInstantiator(
							bootDescriptor,
							bootDescriptor.getComponentClass(),
							runtimeDescriptorAccess,
							creationContext
					);
			instantiatorsByDiscriminator = null;
			instantiatorsByClass = null;
		}
	}

	private Class<?> getEmbeddableClass(
			Component bootDescriptor,
			Map<String, Class<?>> subclassesByName,
			Property property) {
		if ( subclassesByName != null ) {
			final var subclass = subclassesByName.get( bootDescriptor.getPropertyDeclaringClass( property ) );
			return subclass != null ? subclass : getEmbeddableJavaType().getJavaTypeClass();
		}
		else {
			return getEmbeddableJavaType().getJavaTypeClass();
		}
	}

	private static <T> JavaType<?> resolveEmbeddableJavaType(
			Component bootDescriptor,
			CompositeUserType<T> compositeUserType,
			RuntimeModelCreationContext creationContext) {
		final var javaTypeRegistry = creationContext.getTypeConfiguration().getJavaTypeRegistry();
		return compositeUserType == null
				? javaTypeRegistry.resolveDescriptor( bootDescriptor.getComponentClass() )
				: javaTypeRegistry.resolveDescriptor( compositeUserType.returnedClass(),
						() -> new CompositeUserTypeJavaTypeWrapper<>( compositeUserType ) );
	}

	private static EmbeddableInstantiator determineInstantiator(
			Component bootDescriptor,
			Class<?> embeddableClass,
			Supplier<EmbeddableMappingType> runtimeDescriptorAccess,
			RuntimeModelCreationContext creationContext) {
		final var accessorService = creationContext.getServiceRegistry()
				.requireService( PropertyAccessorService.class );
		final HibernateAccessorInstantiator<?> hibernateInstantiator =
				PropertyAccessHelper.resolveInstantiator( embeddableClass, accessorService );
		if ( hibernateInstantiator != null ) {
			return new EmbeddableInstantiatorPojoOptimized(
					embeddableClass,
					runtimeDescriptorAccess,
					hibernateInstantiator
			);
		}
		else if ( bootDescriptor.isEmbedded() && isAbstractClass( embeddableClass ) ) {
			return new EmbeddableInstantiatorProxied(
					embeddableClass,
					runtimeDescriptorAccess,
					getProxyFactoryFactory( creationContext )
							.buildBasicProxyFactory( embeddableClass )
			);
		}
		else {
			return new EmbeddableInstantiatorPojoStandard( embeddableClass, runtimeDescriptorAccess );
		}
	}

	private static ProxyFactoryFactory getProxyFactoryFactory(RuntimeModelCreationContext creationContext) {
		return creationContext.getServiceRegistry()
				.requireService( ProxyFactoryFactory.class );
	}

	private PropertyAccess buildPropertyAccess(
			PropertyAccessorService propertyAccessorService,
			Property property,
			Class<?> embeddableClass,
			boolean requireSetters,
			StrategySelector strategySelector) {
		final var strategy = propertyAccessStrategy( property, embeddableClass, strategySelector );
		if ( strategy == null ) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Could not resolve PropertyAccess for attribute `%s#%s`",
							getEmbeddableJavaType().getTypeName(),
							property.getName()
					)
			);
		}
		return strategy.buildPropertyAccess( propertyAccessorService, embeddableClass, property.getName(), requireSetters );
	}

	private static boolean canBuildMultiValueAccessors(
			Component bootDescriptor,
			boolean hasCustomAccessors) {
		return !hasCustomAccessors
				&& bootDescriptor.getCustomInstantiator() == null
				&& bootDescriptor.getInstantiator() == null
				&& !bootDescriptor.isPolymorphic();
	}

	private static Map<String, Class<?>> getSubclassesByName(
			Component bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		if ( bootDescriptor.isPolymorphic() ) {
			final var subclassNames = bootDescriptor.getDiscriminatorValues().values();
			final Map<String, Class<?>> result = new HashMap<>( subclassNames.size() );
			final var classLoaderService = creationContext.getBootstrapContext().getClassLoaderService();
			for ( final String subclassName : subclassNames ) {
				final var embeddableClass =
						subclassName.equals( bootDescriptor.getComponentClassName() )
								? bootDescriptor.getComponentClass()
								: classLoaderService.classForName( subclassName );
				result.put( subclassName, embeddableClass );
			}
			return result;
		}
		else {
			return null;
		}
	}

	public JavaType<?> getEmbeddableJavaType() {
		return embeddableJavaType;
	}

	@Override
	public JavaType<?> getMappedJavaType() {
		return getEmbeddableJavaType();
	}

	@Override
	public @Nullable HibernateAccessorMultiValueReader getMultiValueReader() {
		return multiValueReader;
	}

	@Override
	public @Nullable HibernateAccessorMultiValueWriter getMultiValueWriter() {
		return multiValueWriter;
	}

	@Override
	public PropertyAccess resolvePropertyAccess(Property bootAttributeDescriptor) {
		return propertyAccesses[ attributeNameToPositionMap.get( bootAttributeDescriptor.getName() ) ];
	}

	@Override
	public RepresentationMode getMode() {
		return RepresentationMode.POJO;
	}

	@Override
	public EmbeddableInstantiator getInstantiator() {
		assert instantiator != null && instantiatorsByDiscriminator == null && instantiatorsByClass == null;
		return instantiator;
	}

	@Override
	public EmbeddableInstantiator getInstantiatorForDiscriminator(Object discriminatorValue) {
		if ( instantiator != null ) {
			assert instantiatorsByDiscriminator == null;
			return instantiator;
		}
		assert instantiatorsByDiscriminator != null;
		return instantiatorsByDiscriminator.get( discriminatorValue );
	}

	@Override
	public EmbeddableInstantiator getInstantiatorForClass(String className) {
		if ( instantiator != null ) {
			assert instantiatorsByClass == null;
			return instantiator;
		}
		assert instantiatorsByClass != null;
		return instantiatorsByClass.get( className );
	}
}
