package org.hibernate.metamodel.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.MappingException;
import org.hibernate.cfg.CheckHandling;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.accessor.Instantiator;
import org.hibernate.accessor.MultiValueReader;
import org.hibernate.accessor.MultiValueWriter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessorService;
import org.hibernate.property.access.spi.SetterFieldImpl;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.internal.CompositeUserTypeJavaTypeWrapper;
import org.hibernate.usertype.CompositeUserType;

import static java.lang.reflect.Modifier.isFinal;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
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

	private final @Nullable MultiValueReader multiValueReader;
	private final @Nullable MultiValueWriter multiValueWriter;
	private final EmbeddableInstantiator instantiator;
	private final Map<Object, EmbeddableInstantiator> instantiatorsByDiscriminator;
	private final Map<String, EmbeddableInstantiator> instantiatorsByClass;

	public EmbeddableRepresentationStrategyPojo(
			@Nonnull Component bootDescriptor,
			@Nonnull Supplier<EmbeddableMappingType> runtimeDescriptorAccess,
			@Nullable EmbeddableInstantiator customInstantiator,
			@Nullable CompositeUserType<Object> compositeUserType,
			@Nonnull RuntimeModelCreationContext creationContext) {
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
		var finalFields = new ArrayList<String>();
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
			if ( propertyAccesses[i].getSetter() instanceof SetterFieldImpl setter
					&& isFinal( setter.getField().getModifiers() ) ) {
				finalFields.add( property.getName() );
			}
		}

		warnOnFinalFields( bootDescriptor, finalFields, customInstantiator,
				creationContext.getSessionFactoryOptions().getFinalPersistentFieldsHandling() );

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

	@Nonnull
	private Class<?> getEmbeddableClass(
			@Nonnull Component bootDescriptor,
			@Nullable Map<String, Class<?>> subclassesByName,
			@Nonnull Property property) {
		if ( subclassesByName != null ) {
			final var subclass = subclassesByName.get( bootDescriptor.getPropertyDeclaringClass( property ) );
			return subclass != null ? subclass : getEmbeddableJavaType().getJavaTypeClass();
		}
		else {
			return getEmbeddableJavaType().getJavaTypeClass();
		}
	}

	@Nonnull
	private static <T> JavaType<?> resolveEmbeddableJavaType(
			@Nonnull Component bootDescriptor,
			@Nullable CompositeUserType<T> compositeUserType,
			@Nonnull RuntimeModelCreationContext creationContext) {
		final var javaTypeRegistry = creationContext.getTypeConfiguration().getJavaTypeRegistry();
		return compositeUserType == null
				? javaTypeRegistry.resolveDescriptor( bootDescriptor.getComponentClass() )
				: javaTypeRegistry.resolveDescriptor( compositeUserType.returnedClass(),
						() -> new CompositeUserTypeJavaTypeWrapper<>( compositeUserType ) );
	}

	@Nonnull
	private static EmbeddableInstantiator determineInstantiator(
			@Nonnull Component bootDescriptor,
			@Nonnull Class<?> embeddableClass,
			@Nonnull Supplier<EmbeddableMappingType> runtimeDescriptorAccess,
			@Nonnull RuntimeModelCreationContext creationContext) {
		final var accessorService = creationContext.getServiceRegistry()
				.requireService( PropertyAccessorService.class );
		final Instantiator<?> hibernateInstantiator =
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
			return new EmbeddableInstantiatorPojoStandard( embeddableClass, runtimeDescriptorAccess, accessorService.hibernateAccessorFactory() );
		}
	}

	@Nonnull
	private static ProxyFactoryFactory getProxyFactoryFactory(@Nonnull RuntimeModelCreationContext creationContext) {
		return creationContext.getServiceRegistry()
				.requireService( ProxyFactoryFactory.class );
	}

	@Nonnull
	private PropertyAccess buildPropertyAccess(
			@Nonnull PropertyAccessorService propertyAccessorService,
			@Nonnull Property property,
			@Nonnull Class<?> embeddableClass,
			boolean requireSetters,
			@Nonnull StrategySelector strategySelector) {
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
			@Nonnull Component bootDescriptor,
			boolean hasCustomAccessors) {
		return !hasCustomAccessors
				&& bootDescriptor.getCustomInstantiator() == null
				&& bootDescriptor.getInstantiator() == null
				&& !bootDescriptor.isPolymorphic();
	}

	private static void warnOnFinalFields(
			@Nonnull Component bootDescriptor,
			@Nonnull List<String> finalFields,
			@Nullable EmbeddableInstantiator customInstantiator,
			@Nonnull CheckHandling handling) {
		if ( customInstantiator != null && !( customInstantiator instanceof StandardEmbeddableInstantiator ) ) {
			return;
		}
		if ( handling == CheckHandling.IGNORE || finalFields.isEmpty() ) {
			return;
		}
		if ( handling == CheckHandling.ERROR ) {
			throw new MappingException( String.format(
					"Persistent fields %s in embeddable class '%s' are declared 'final'",
					finalFields, bootDescriptor.getComponentClassName()
			) );
		}
		CORE_LOGGER.finalPersistentFields(
				finalFields, "embeddable", bootDescriptor.getComponentClassName(),
				MappingSettings.FINAL_PERSISTENT_FIELDS
		);
	}

	@Nullable
	private static Map<String, Class<?>> getSubclassesByName(
			@Nonnull Component bootDescriptor,
			@Nonnull RuntimeModelCreationContext creationContext) {
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

	@Nonnull
	public JavaType<?> getEmbeddableJavaType() {
		return embeddableJavaType;
	}

	@Override
	@Nonnull
	public JavaType<?> getMappedJavaType() {
		return getEmbeddableJavaType();
	}

	@Override
	public @Nullable MultiValueReader getMultiValueReader() {
		return multiValueReader;
	}

	@Override
	public @Nullable MultiValueWriter getMultiValueWriter() {
		return multiValueWriter;
	}

	@Override
	@Nonnull
	public PropertyAccess resolvePropertyAccess(@Nonnull Property bootAttributeDescriptor) {
		return propertyAccesses[ attributeNameToPositionMap.get( bootAttributeDescriptor.getName() ) ];
	}

	@Override
	@Nonnull
	public RepresentationMode getMode() {
		return RepresentationMode.POJO;
	}

	@Override
	@Nonnull
	public EmbeddableInstantiator getInstantiator() {
		assert instantiator != null && instantiatorsByDiscriminator == null && instantiatorsByClass == null;
		return instantiator;
	}

	@Override
	@Nullable
	public EmbeddableInstantiator getInstantiatorForDiscriminator(@Nullable Object discriminatorValue) {
		if ( instantiator != null ) {
			assert instantiatorsByDiscriminator == null;
			return instantiator;
		}
		assert instantiatorsByDiscriminator != null;
		return instantiatorsByDiscriminator.get( discriminatorValue );
	}

	@Override
	@Nullable
	public EmbeddableInstantiator getInstantiatorForClass(@Nonnull String className) {
		if ( instantiator != null ) {
			assert instantiatorsByClass == null;
			return instantiator;
		}
		assert instantiatorsByClass != null;
		return instantiatorsByClass.get( className );
	}
}
