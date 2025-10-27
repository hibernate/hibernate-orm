/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Backref;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IndexBackref;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyIndexBackRefImpl;
import org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.internal.CompositeUserTypeJavaTypeWrapper;
import org.hibernate.usertype.CompositeUserType;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

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
		this.embeddableJavaType = resolveEmbeddableJavaType( bootDescriptor, compositeUserType, creationContext );

		final int propertySpan = bootDescriptor.getPropertySpan();
		this.propertyAccesses = new PropertyAccess[propertySpan];
		this.attributeNameToPositionMap = new HashMap<>( propertySpan );

		// We need access to the Class objects, used only during initialization
		final Map<String, Class<?>> subclassesByName = getSubclassesByName( bootDescriptor, creationContext );
		boolean foundCustomAccessor = false;
		for ( int i = 0; i < bootDescriptor.getProperties().size(); i++ ) {
			final Property property = bootDescriptor.getProperty( i );
			final Class<?> embeddableClass;
			if ( bootDescriptor.isPolymorphic() ) {
				final Class<?> subclass = castNonNull( subclassesByName ).get( bootDescriptor.getPropertyDeclaringClass( property ) );
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
		this.strategySelector = creationContext.getServiceRegistry().getService( StrategySelector.class );
		this.reflectionOptimizer = buildReflectionOptimizer(
				bootDescriptor,
				hasCustomAccessors,
				propertyAccesses,
				creationContext
		);

		if ( bootDescriptor.isPolymorphic() ) {
			final int size = bootDescriptor.getDiscriminatorValues().size();
			this.instantiatorsByDiscriminator = new HashMap<>( size );
			this.instantiatorsByClass = new HashMap<>( size );
			for ( Map.Entry<Object, String> discriminator : bootDescriptor.getDiscriminatorValues().entrySet() ) {
				final String className = discriminator.getValue();
				final EmbeddableInstantiator instantiator = determineInstantiator(
						bootDescriptor,
						castNonNull( subclassesByName ).get( className ),
						reflectionOptimizer,
						runtimeDescriptorAccess,
						creationContext
				);
				instantiatorsByDiscriminator.put( discriminator.getKey(), instantiator );
				instantiatorsByClass.put( className, instantiator );
			}
			this.instantiator = null;
		}
		else {
			this.instantiator = customInstantiator != null ?
					customInstantiator :
					determineInstantiator(
							bootDescriptor,
							bootDescriptor.getComponentClass(),
							reflectionOptimizer,
							runtimeDescriptorAccess,
							creationContext
					);
			this.instantiatorsByDiscriminator = null;
			this.instantiatorsByClass = null;
		}
	}

	private static <T> JavaType<T> resolveEmbeddableJavaType(
			Component bootDescriptor,
			CompositeUserType<T> compositeUserType,
			RuntimeModelCreationContext creationContext) {
		final JavaTypeRegistry javaTypeRegistry = creationContext.getTypeConfiguration().getJavaTypeRegistry();
		if ( compositeUserType == null ) {
			return javaTypeRegistry.resolveDescriptor( bootDescriptor.getComponentClass() );
		}
		return javaTypeRegistry.resolveDescriptor(
				compositeUserType.returnedClass(),
				() -> new CompositeUserTypeJavaTypeWrapper<>( compositeUserType )
		);
	}

	private static EmbeddableInstantiator determineInstantiator(
			Component bootDescriptor,
			Class<?> embeddableClass,
			ReflectionOptimizer reflectionOptimizer,
			Supplier<EmbeddableMappingType> runtimeDescriptorAccess,
			RuntimeModelCreationContext creationContext) {
		if ( reflectionOptimizer != null && reflectionOptimizer.getInstantiationOptimizer() != null ) {
			final ReflectionOptimizer.InstantiationOptimizer instantiationOptimizer = reflectionOptimizer.getInstantiationOptimizer();
			return new EmbeddableInstantiatorPojoOptimized(
					embeddableClass,
					runtimeDescriptorAccess,
					instantiationOptimizer
			);
		}

		if ( bootDescriptor.isEmbedded() && ReflectHelper.isAbstractClass( embeddableClass ) ) {
			return new EmbeddableInstantiatorProxied(
					embeddableClass,
					runtimeDescriptorAccess,
					creationContext.getServiceRegistry()
							.requireService( ProxyFactoryFactory.class )
							.buildBasicProxyFactory( embeddableClass )
			);
		}

		return new EmbeddableInstantiatorPojoStandard( embeddableClass, runtimeDescriptorAccess );
	}

	private PropertyAccess buildPropertyAccess(
			Property bootAttributeDescriptor,
			Class<?> embeddableClass,
			boolean requireSetters) {
		PropertyAccessStrategy strategy = bootAttributeDescriptor.getPropertyAccessStrategy( embeddableClass );

		if ( strategy == null ) {
			final String propertyAccessorName = bootAttributeDescriptor.getPropertyAccessorName();
			if ( StringHelper.isNotEmpty( propertyAccessorName ) ) {

				// handle explicitly specified attribute accessor
				strategy = strategySelector.resolveStrategy(
						PropertyAccessStrategy.class,
						propertyAccessorName
				);
			}
			else {
				if ( bootAttributeDescriptor instanceof Backref ) {
					final Backref backref = (Backref) bootAttributeDescriptor;
					strategy = new PropertyAccessStrategyBackRefImpl( backref.getCollectionRole(), backref
							.getEntityName() );
				}
				else if ( bootAttributeDescriptor instanceof IndexBackref ) {
					final IndexBackref indexBackref = (IndexBackref) bootAttributeDescriptor;
					strategy = new PropertyAccessStrategyIndexBackRefImpl(
							indexBackref.getCollectionRole(),
							indexBackref.getEntityName()
					);
				}
				else {
					// for now...
					strategy = BuiltInPropertyAccessStrategies.MIXED.getStrategy();
				}
			}
		}

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

		return strategy.buildPropertyAccess(
				embeddableClass,
				bootAttributeDescriptor.getName(),
				requireSetters
		);
	}

	private static ReflectionOptimizer buildReflectionOptimizer(
			Component bootDescriptor,
			boolean hasCustomAccessors,
			PropertyAccess[] propertyAccesses,
			RuntimeModelCreationContext creationContext) {
		if ( hasCustomAccessors || bootDescriptor.getCustomInstantiator() != null || bootDescriptor.getInstantiator() != null || bootDescriptor.isPolymorphic() ) {
			return null;
		}

		final List<Property> properties = bootDescriptor.getProperties();
			final BytecodeProvider.PropertyInfo[] propertyInfos = new BytecodeProvider.PropertyInfo[properties.size()];

		for ( int i = 0; i < properties.size(); i++ ) {
		final Property property = properties.get( i );
			propertyInfos[i] = new BytecodeProvider.PropertyInfo( property.getName(), propertyAccesses[i] );

		}

		return creationContext.getServiceRegistry()
				.requireService( BytecodeProvider.class )
				.getReflectionOptimizer( bootDescriptor.getComponentClass(), propertyInfos );
	}

	private static Map<String, Class<?>> getSubclassesByName(
			Component bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		if ( bootDescriptor.isPolymorphic() ) {
			final Collection<String> subclassNames = bootDescriptor.getDiscriminatorValues().values();
			final Map<String, Class<?>> result = new HashMap<>( subclassNames.size() );
			final ClassLoaderService classLoaderService = creationContext.getMetadata()
					.getMetadataBuildingOptions()
					.getServiceRegistry()
					.requireService( ClassLoaderService.class );
			for ( final String subclassName : subclassNames ) {
				final Class<?> embeddableClass;
				if ( subclassName.equals( bootDescriptor.getComponentClassName() ) ) {
					embeddableClass = bootDescriptor.getComponentClass();
				}
				else {
					embeddableClass = classLoaderService.classForName( subclassName );
				}
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
