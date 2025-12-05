/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java.spi;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.type.descriptor.java.ArrayJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;

import org.jboss.logging.Logger;

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
		final JavaType<?> old = descriptorsByTypeName.put( descriptor.getJavaType().getTypeName(), descriptor );
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

	@Deprecated(since = "7.2") // Due to unbound type parameter
	public <T> JavaType<T> getDescriptor(Type javaType) {
		//noinspection unchecked
		return (JavaType<T>) resolveDescriptor( javaType );
	}

	public JavaType<?> findDescriptor(Type javaType) {
		return descriptorsByTypeName.get( javaType.getTypeName() );
	}

	public <J> JavaType<J> findDescriptor(Class<J> javaType) {
		//noinspection unchecked
		return (JavaType<J>) findDescriptor( (Type) javaType );
	}

	public <J> JavaType<J> resolveDescriptor(Class<? extends J> javaType, Supplier<JavaType<J>> creator) {
		//noinspection unchecked
		return (JavaType<J>) resolveDescriptor( javaType.getTypeName(), creator );
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
		//noinspection unchecked
		return (JavaType<J>) resolveDescriptor( javaType, JavaTypeRegistry::createMutabilityPlan );
	}

	public <J> JavaType<J> resolveDescriptor(JavaType<J> javaType) {
		//noinspection unchecked
		return (JavaType<J>) resolveDescriptor( javaType.getJavaTypeClass().getTypeName(), () -> javaType );
	}

	private static MutabilityPlan<?> createMutabilityPlan(Type elementJavaType, TypeConfiguration typeConfiguration) {
		final MutabilityPlan<?> determinedPlan = RegistryHelper.INSTANCE.determineMutabilityPlan(
				elementJavaType,
				typeConfiguration
		);
		if ( determinedPlan != null ) {
			return determinedPlan;
		}

		return MutableMutabilityPlan.INSTANCE;
	}

	public <T> JavaType<T[]> resolveArrayDescriptor(Class<T> elementJavaType) {
		//noinspection unchecked
		return (JavaType<T[]>)
				resolveDescriptor( elementJavaType.getTypeName() + "[]",
						() -> createArrayTypeDescriptor( elementJavaType, JavaTypeRegistry::createMutabilityPlan) );
	}

	@Deprecated(since = "7.2", forRemoval = true) // Can be private
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
		JavaType<J> elementTypeDescriptor = findDescriptor( elementJavaType );
		if ( elementTypeDescriptor == null ) {
			elementTypeDescriptor = createTypeDescriptor( elementJavaType, mutabilityPlanCreator );
		}
		return new ArrayJavaType<>( elementTypeDescriptor );
	}

	private <J> JavaType<J> createTypeDescriptor(Type javaType, BiFunction<Type, TypeConfiguration, MutabilityPlan<?>> mutabilityPlanCreator) {
		//noinspection unchecked
		return RegistryHelper.INSTANCE.createTypeDescriptor(
				javaType,
				() -> (MutabilityPlan<J>) mutabilityPlanCreator.apply( javaType, typeConfiguration ),
				typeConfiguration
		);
	}

	public JavaType<?> resolveManagedTypeDescriptor(Type javaType) {
		return resolveManagedTypeDescriptor( javaType, false );
	}

	public <J> JavaType<J> resolveManagedTypeDescriptor(Class<J> javaType) {
		//noinspection unchecked
		return (JavaType<J>) resolveManagedTypeDescriptor( javaType, false );
	}

	public JavaType<?> resolveEntityTypeDescriptor(Type javaType) {
		return resolveManagedTypeDescriptor( javaType, true );
	}

	public <J> JavaType<J> resolveEntityTypeDescriptor(Class<J> javaType) {
		//noinspection unchecked
		return (JavaType<J>) resolveManagedTypeDescriptor( javaType, true );
	}

	private <J> JavaType<?> resolveManagedTypeDescriptor(Type javaType, boolean entity) {
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

					final MutabilityPlan<J> determinedPlan = RegistryHelper.INSTANCE.determineMutabilityPlan(
							javaType,
							typeConfiguration
					);
					final MutabilityPlan<J> mutabilityPlan =
							determinedPlan != null
									? determinedPlan
									: (MutabilityPlan<J>) MutableMutabilityPlan.INSTANCE;

					return entity
							? new EntityJavaType<>( javaTypeClass, mutabilityPlan )
							: new JavaTypeBasicAdaptor<>( javaTypeClass, mutabilityPlan );
				}
		);
	}
}
