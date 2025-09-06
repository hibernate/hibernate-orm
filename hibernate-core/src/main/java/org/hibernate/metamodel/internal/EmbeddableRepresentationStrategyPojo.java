/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.property.access.spi.PropertyAccess;
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

	private final StrategySelector strategySelector;
	private final ReflectionOptimizer reflectionOptimizer;
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

		// We need access to the Class objects, used only during initialization
		final var subclassesByName = getSubclassesByName( bootDescriptor, creationContext );
		boolean foundCustomAccessor = false;
		for ( int i = 0; i < bootDescriptor.getProperties().size(); i++ ) {
			final Property property = bootDescriptor.getProperty( i );
			final Class<?> embeddableClass;
			if ( subclassesByName != null ) {
				final var subclass = subclassesByName.get( bootDescriptor.getPropertyDeclaringClass( property ) );
				embeddableClass = subclass != null ? subclass : getEmbeddableJavaType().getJavaTypeClass();
			}
			else {
				embeddableClass = getEmbeddableJavaType().getJavaTypeClass();
			}
			propertyAccesses[i] = buildPropertyAccess( property, embeddableClass, customInstantiator == null );
			attributeNameToPositionMap.put( property.getName(), i );

			if ( !property.isBasicPropertyAccessor() ) {
				foundCustomAccessor = true;
			}
		}

		boolean hasCustomAccessors = foundCustomAccessor;
		strategySelector = creationContext.getServiceRegistry().getService( StrategySelector.class );
		reflectionOptimizer = buildReflectionOptimizer(
				bootDescriptor,
				hasCustomAccessors,
				propertyAccesses,
				creationContext
		);

		if ( bootDescriptor.isPolymorphic() ) {
			final int size = bootDescriptor.getDiscriminatorValues().size();
			instantiatorsByDiscriminator = new HashMap<>( size );
			instantiatorsByClass = new HashMap<>( size );
			for ( var discriminator : bootDescriptor.getDiscriminatorValues().entrySet() ) {
				final String className = discriminator.getValue();
				final var instantiator = determineInstantiator(
						bootDescriptor,
						castNonNull( subclassesByName ).get( className ),
						reflectionOptimizer,
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
							reflectionOptimizer,
							runtimeDescriptorAccess,
							creationContext
					);
			instantiatorsByDiscriminator = null;
			instantiatorsByClass = null;
		}
	}

	private static <T> JavaType<T> resolveEmbeddableJavaType(
			Component bootDescriptor,
			CompositeUserType<T> compositeUserType,
			RuntimeModelCreationContext creationContext) {
		final var javaTypeRegistry = creationContext.getTypeConfiguration().getJavaTypeRegistry();
		if ( compositeUserType == null ) {
			return javaTypeRegistry.getDescriptor( bootDescriptor.getComponentClass() );
		}
		else {
			return javaTypeRegistry.resolveDescriptor( compositeUserType.returnedClass(),
					() -> new CompositeUserTypeJavaTypeWrapper<>( compositeUserType ) );
		}
	}

	private static EmbeddableInstantiator determineInstantiator(
			Component bootDescriptor,
			Class<?> embeddableClass,
			ReflectionOptimizer reflectionOptimizer,
			Supplier<EmbeddableMappingType> runtimeDescriptorAccess,
			RuntimeModelCreationContext creationContext) {
		if ( reflectionOptimizer != null && reflectionOptimizer.getInstantiationOptimizer() != null ) {
			return new EmbeddableInstantiatorPojoOptimized(
					embeddableClass,
					runtimeDescriptorAccess,
					reflectionOptimizer.getInstantiationOptimizer()
			);
		}

		if ( bootDescriptor.isEmbedded() && isAbstractClass( embeddableClass ) ) {
			return new EmbeddableInstantiatorProxied(
					embeddableClass,
					runtimeDescriptorAccess,
					getProxyFactoryFactory( creationContext )
							.buildBasicProxyFactory( embeddableClass )
			);
		}

		return new EmbeddableInstantiatorPojoStandard( embeddableClass, runtimeDescriptorAccess );
	}

	private static ProxyFactoryFactory getProxyFactoryFactory(RuntimeModelCreationContext creationContext) {
		return creationContext.getServiceRegistry()
				.requireService( ProxyFactoryFactory.class );
	}

	private PropertyAccess buildPropertyAccess(
			Property bootAttributeDescriptor,
			Class<?> embeddableClass,
			boolean requireSetters) {
		final var strategy = propertyAccessStrategy( bootAttributeDescriptor, embeddableClass, strategySelector );
		if ( strategy == null ) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Could not resolve PropertyAccess for attribute `%s#%s`",
							getEmbeddableJavaType().getTypeName(),
							bootAttributeDescriptor.getName()
					)
			);
		}
		return strategy.buildPropertyAccess( embeddableClass, bootAttributeDescriptor.getName(), requireSetters );
	}

	private static ReflectionOptimizer buildReflectionOptimizer(
			Component bootDescriptor,
			boolean hasCustomAccessors,
			PropertyAccess[] propertyAccesses,
			RuntimeModelCreationContext creationContext) {
		if ( !hasCustomAccessors
				&& bootDescriptor.getCustomInstantiator() == null
				&& bootDescriptor.getInstantiator() == null
				&& !bootDescriptor.isPolymorphic() ) {
			final Map<String, PropertyAccess> propertyAccessMap = new LinkedHashMap<>();
			int i = 0;
			for ( Property property : bootDescriptor.getProperties() ) {
				propertyAccessMap.put( property.getName(), propertyAccesses[i] );
				i++;
			}
			return creationContext.getServiceRegistry()
					.requireService( BytecodeProvider.class )
					.getReflectionOptimizer( bootDescriptor.getComponentClass(), propertyAccessMap );
		}
		else {
			return null;
		}

	}

	private static Map<String, Class<?>> getSubclassesByName(
			Component bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		if ( bootDescriptor.isPolymorphic() ) {
			final Collection<String> subclassNames = bootDescriptor.getDiscriminatorValues().values();
			final Map<String, Class<?>> result = new HashMap<>( subclassNames.size() );
			final ClassLoaderService classLoaderService = creationContext.getBootstrapContext().getClassLoaderService();
			for ( final String subclassName : subclassNames ) {
				final Class<?> embeddableClass =
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
	public ReflectionOptimizer getReflectionOptimizer() {
		return reflectionOptimizer;
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
