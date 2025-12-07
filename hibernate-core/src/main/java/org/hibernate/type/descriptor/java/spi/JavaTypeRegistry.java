/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java.spi;

import org.hibernate.Internal;
import org.hibernate.type.descriptor.java.ArrayJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;
import org.jboss.logging.Logger;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hibernate.internal.util.type.PrimitiveWrappers.canonicalize;


/**
 * A registry mapping {@link Class Java classes} to implementations
 * of the {@link JavaType} interface.
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 *
 * @since 5.3
 */
public class JavaTypeRegistry implements JavaTypeBaseline.BaselineTarget, Serializable {
	private static final Logger LOG = Logger.getLogger( JavaTypeRegistry.class );

	private final TypeConfiguration typeConfiguration;
	private final ConcurrentHashMap<String, JavaType<?>> descriptorsByTypeName = new ConcurrentHashMap<>();

	public JavaTypeRegistry(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
		JavaTypeBaseline.prime( this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// baseline descriptors

	@Override
	public void addBaselineDescriptor(JavaType<?> descriptor) {
		if ( descriptor.getJavaType() == null ) {
			throw new IllegalStateException( "Illegal to add BasicJavaType with null Java type" );
		}
		addBaselineDescriptor( descriptor.getJavaType(), descriptor );
	}

	@Override
	public void addBaselineDescriptor(Type describedJavaType, JavaType<?> descriptor) {
		performInjections( descriptor );
		descriptorsByTypeName.put( describedJavaType.getTypeName(), descriptor );
	}

	private void performInjections(JavaType<?> descriptor) {
		if ( descriptor instanceof TypeConfigurationAware typeConfigurationAware ) {
			// would be nice to make the JavaType for an entity, e.g., aware of the TypeConfiguration
			typeConfigurationAware.setTypeConfiguration( typeConfiguration );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// descriptor access

	public void forEachDescriptor(Consumer<JavaType<?>> consumer) {
		descriptorsByTypeName.values().forEach( consumer );
	}

	public void addDescriptor(JavaType<?> descriptor) {
		final var old = descriptorsByTypeName.put( descriptor.getJavaType().getTypeName(), descriptor );
		if ( old != null ) {
			LOG.debugf(
					"JavaTypeRegistry entry replaced : %s -> %s (was %s)",
					descriptor.getJavaType(),
					descriptor,
					old
			);
		}
		performInjections( descriptor );
	}

	// This method is terribly lacking in type safety but
	// has wormed itself into so many clients that I can't
	// easily do away with it right now
	@Deprecated(since = "7.2") // Due to unbound type parameter and unchecked cast
	public <T> JavaType<T> getDescriptor(Type javaType) {
		//noinspection unchecked
		return (JavaType<T>) resolveDescriptor( javaType );
	}

	public JavaType<?> findDescriptor(Type javaType) {
		return descriptorsByTypeName.get( javaType.getTypeName() );
	}

	public <J> JavaType<J> findDescriptor(Class<J> javaClass) {
		final var cached = descriptorsByTypeName.get( javaClass.getTypeName() );
		return cached == null ? null : checkCached( javaClass, cached );
	}

	public <J> JavaType<J> resolveDescriptor(Class<? extends J> javaType, Supplier<JavaType<J>> creator) {
		final String javaTypeName = javaType.getTypeName();
		final var cached = descriptorsByTypeName.get( javaTypeName );
		if ( cached != null ) {
			return checkCached( javaType, cached );
		}
		else {
			final var created = creator.get();
			descriptorsByTypeName.put( javaTypeName, created );
			return created;
		}
	}

	private static <J> JavaType<J> checkCached(Class<? extends J> javaClass, JavaType<?> cached) {
		final var cachedClass = cached.getJavaTypeClass();
		if ( !isCompatible( javaClass, cachedClass ) ) {
			throw new IllegalStateException( "Type registration was corrupted for: " + javaClass.getName() );
		}
		@SuppressWarnings("unchecked") // safe, we just checked
		final var resolvedType = (JavaType<J>) cached;
		return resolvedType;
	}

	private static boolean isCompatible(Class<?> givenClass, Class<?> cachedClass) {
		return cachedClass == canonicalize( givenClass );
	}

	@Deprecated(since = "7.2", forRemoval = true) // Can be private
	private JavaType<?> resolveDescriptor(String javaTypeName, Supplier<? extends JavaType<?>> creator) {
		final var cached = descriptorsByTypeName.get( javaTypeName );
		if ( cached != null ) {
			return cached;
		}
		else {
			final var created = creator.get();
			descriptorsByTypeName.put( javaTypeName, created );
			return created;
		}
	}

	public JavaType<?> resolveDescriptor(Type javaType) {
		return resolveDescriptor( javaType, JavaTypeRegistry::createMutabilityPlan );
	}

	public <J> JavaType<J> resolveDescriptor(Class<J> javaType) {
		return resolveDescriptor( javaType, JavaTypeRegistry::createMutabilityPlan );
	}

	public <J> JavaType<J> resolveDescriptor(JavaType<J> javaType) {
		return resolveDescriptor( javaType.getJavaTypeClass(), () -> javaType );
	}

	private static MutabilityPlan<?> createMutabilityPlan(Type elementJavaType, TypeConfiguration typeConfiguration) {
		final var determinedPlan =
				RegistryHelper.INSTANCE.determineMutabilityPlan( elementJavaType, typeConfiguration );
		return determinedPlan != null ? determinedPlan : MutableMutabilityPlan.instance();

	}

	public <T> JavaType<T[]> resolveArrayDescriptor(Class<T> elementJavaType) {
		//noinspection unchecked
		return (JavaType<T[]>)
				resolveDescriptor( elementJavaType.getTypeName() + "[]",
						() -> createArrayTypeDescriptor( elementJavaType, JavaTypeRegistry::createMutabilityPlan) );
	}

	// WAS:
//	public <T> JavaType<T[]> resolveArrayDescriptor(Class<T> elementJavaType) {
//		return resolveDescriptor( arrayClass( elementJavaType ),
//				() -> createArrayTypeDescriptor( elementJavaType, JavaTypeRegistry::createMutabilityPlan) );
//	}

	@Internal // Can be demoted to private
	public <J> JavaType<J> resolveDescriptor(
			Class<J> javaType,
			BiFunction<Type, TypeConfiguration, MutabilityPlan<?>> mutabilityPlanCreator) {
		//noinspection unchecked
		return resolveDescriptor(
				javaType,
				() -> javaType.isArray()
						? (JavaType<J>) createArrayTypeDescriptor( javaType.getComponentType(), mutabilityPlanCreator )
						: createTypeDescriptor( javaType, mutabilityPlanCreator )
		);
	}

	@Internal // Can be demoted to private
	public JavaType<?> resolveDescriptor(
			Type javaType,
			BiFunction<Type, TypeConfiguration, MutabilityPlan<?>> mutabilityPlanCreator) {
		return resolveDescriptor(
				javaType.getTypeName(),
				() -> {
					if ( javaType instanceof ParameterizedType parameterizedType ) {
						final var rawType = findDescriptor( parameterizedType.getRawType() );
						if ( rawType != null ) {
							return rawType.createJavaType( parameterizedType, typeConfiguration );
						}
					}
					else if ( javaType instanceof Class<?> javaClass && javaClass.isArray() ) {
						return createArrayTypeDescriptor( javaClass.getComponentType(), mutabilityPlanCreator );
					}
					return createTypeDescriptor( javaType, mutabilityPlanCreator );
				}
		);
	}

	private <J> JavaType<J[]> createArrayTypeDescriptor(Class<J> elementJavaType, BiFunction<Type, TypeConfiguration, MutabilityPlan<?>> mutabilityPlanCreator) {
		var elementTypeDescriptor = findDescriptor( elementJavaType );
		if ( elementTypeDescriptor == null ) {
			elementTypeDescriptor = createTypeDescriptor( elementJavaType, mutabilityPlanCreator );
		}
		return new ArrayJavaType<>( elementTypeDescriptor );
	}

	private <J> JavaType<J> createTypeDescriptor(Type javaType, BiFunction<Type, TypeConfiguration, MutabilityPlan<?>> mutabilityPlanCreator) {
		//noinspection unchecked
		return RegistryHelper.INSTANCE.createTypeDescriptor(
				javaType,
				() -> (MutabilityPlan<J>)
						mutabilityPlanCreator.apply( javaType, typeConfiguration ),
				typeConfiguration
		);
	}

	public <J> JavaType<J> resolveManagedTypeDescriptor(Class<J> javaType) {
		return resolveManagedTypeDescriptor( javaType, JavaTypeBasicAdaptor::new );
	}

	public <J> JavaType<J> resolveEntityTypeDescriptor(Class<J> javaType) {
		return resolveManagedTypeDescriptor( javaType, EntityJavaType::new );
	}

	private <J> JavaType<J> resolveManagedTypeDescriptor(
			Class<J> javaType,
			BiFunction<Class<J>, MutabilityPlan<J>, JavaType<J>> instantiate) {
		return resolveDescriptor(
				javaType,
				() -> {
					final var determinedPlan =
							RegistryHelper.INSTANCE.determineMutabilityPlan( javaType, typeConfiguration );
					return instantiate.apply(
							javaType,
							determinedPlan != null
									? determinedPlan
									: MutableMutabilityPlan.instance()
					);
				}
		);
	}

	// CAN BE REMOVED:

	@Deprecated(since = "7.2", forRemoval = true) // no longer used
	public JavaType<?> resolveManagedTypeDescriptor(Type javaType) {
		return resolveManagedTypeDescriptor( javaType, JavaTypeBasicAdaptor::new );
	}

	@Deprecated(since = "7.2", forRemoval = true) // no longer used
	public JavaType<?> resolveEntityTypeDescriptor(Type javaType) {
		return resolveManagedTypeDescriptor( javaType, EntityJavaType::new );
	}

	@Deprecated(since = "7.2", forRemoval = true) // no longer used
	@SuppressWarnings("unchecked")
	private <J> JavaType<?> resolveManagedTypeDescriptor(
			Type javaType,
			BiFunction<Class<J>, MutabilityPlan<J>, JavaType<J>> instantiate) {
		return resolveDescriptor(
				javaType.getTypeName(),
				() -> {
					final Class<J> javaTypeClass;
					if ( javaType instanceof Class<?> ) {
						javaTypeClass = (Class<J>) javaType;
					}
					else {
						final var parameterizedType = (ParameterizedType) javaType;
						javaTypeClass = (Class<J>) parameterizedType.getRawType();
					}

					final var determinedPlan =
							RegistryHelper.INSTANCE.determineMutabilityPlan( javaTypeClass, typeConfiguration );
					final MutabilityPlan<J> mutabilityPlan =
							determinedPlan != null
									? determinedPlan
									: MutableMutabilityPlan.instance();
					return instantiate.apply( javaTypeClass, mutabilityPlan );
				}
		);
	}
}
