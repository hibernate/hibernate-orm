/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.spi;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.Nullable;
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
	private static final Logger log = Logger.getLogger( JavaTypeRegistry.class );

	private final TypeConfiguration typeConfiguration;
	private final ConcurrentHashMap<Type, JavaType<?>> descriptorsByType = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, ConcurrentHashMap<Type, JavaType<?>>> typeCodeSpecificDescriptorsByType = new ConcurrentHashMap<>();

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
		descriptorsByType.put( describedJavaType, descriptor );
	}

	private void performInjections(JavaType<?> descriptor) {
		if ( descriptor instanceof TypeConfigurationAware ) {
			// would be nice to make the JavaType for an entity, e.g., aware of the TypeConfiguration
			( (TypeConfigurationAware) descriptor ).setTypeConfiguration( typeConfiguration );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// descriptor access

	public void forEachDescriptor(Consumer<JavaType<?>> consumer) {
		descriptorsByType.values().forEach( consumer );
		typeCodeSpecificDescriptorsByType.values().forEach( descriptorsByTypeName -> {
			descriptorsByTypeName.values().forEach( consumer );
		} );
	}

	public <T> JavaType<T> getDescriptor(Type javaType) {
		return resolveDescriptor( javaType );
	}

	public void addDescriptor(JavaType<?> descriptor) {
		addDescriptor( descriptorsByType, descriptor );
	}

	public void addDescriptor(int sqlTypeCode, JavaType<?> descriptor) {
		final ConcurrentHashMap<Type, JavaType<?>> descriptorsByTypeName =
				typeCodeSpecificDescriptorsByType.computeIfAbsent( sqlTypeCode, k -> new ConcurrentHashMap<>() );
		addDescriptor( descriptorsByTypeName, descriptor );
	}

	private void addDescriptor(ConcurrentHashMap<Type, JavaType<?>> descriptorsByType, JavaType<?> descriptor) {
		JavaType<?> old = descriptorsByType.put( descriptor.getJavaType(), descriptor );
		if ( old != null ) {
			log.debugf(
					"JavaTypeRegistry entry replaced : %s -> %s (was %s)",
					descriptor.getJavaType(),
					descriptor,
					old
			);
		}
		performInjections( descriptor );
	}

	public <J> @Nullable JavaType<J> findDescriptor(Type javaType) {
		//noinspection unchecked
		return (JavaType<J>) descriptorsByType.get( javaType );
	}

	public @Nullable JavaType<?> findDescriptor(int sqlTypeCode, Type javaType) {
		final ConcurrentHashMap<Type, JavaType<?>> descriptorsByType =
				typeCodeSpecificDescriptorsByType.get( sqlTypeCode );
		return descriptorsByType.get( javaType );
	}

	public <J> JavaType<J> resolveDescriptor(Type javaType, Supplier<JavaType<J>> creator) {
		//noinspection unchecked
		return (JavaType<J>) resolveDescriptor( descriptorsByType, javaType, creator );
	}

	public JavaType<?> resolveDescriptor(int sqlTypeCode, Type javaType, Supplier<JavaType<?>> creator) {
		final ConcurrentHashMap<Type, JavaType<?>> descriptorsByType =
				typeCodeSpecificDescriptorsByType.computeIfAbsent( sqlTypeCode, k -> new ConcurrentHashMap<>() );
		return resolveDescriptor( descriptorsByType, javaType, creator );
	}

	private JavaType<?> resolveDescriptor(ConcurrentHashMap<Type, JavaType<?>> descriptorsByType, Type javaType, Supplier<? extends JavaType<?>> creator) {
		final JavaType<?> cached = descriptorsByType.get( javaType );
		if ( cached != null ) {
			return cached;
		}

		final JavaType<?> created = creator.get();
		descriptorsByType.put( javaType, created );
		return created;
	}

	public <J> JavaType<J> resolveDescriptor(Type javaType) {
		return resolveDescriptor( javaType, (elementJavaType, typeConfiguration) -> {
			final MutabilityPlan<J> determinedPlan = RegistryHelper.INSTANCE.determineMutabilityPlan(
					elementJavaType,
					typeConfiguration
			);
			if ( determinedPlan != null ) {
				return determinedPlan;
			}

			return MutableMutabilityPlan.INSTANCE;
		} );
	}

	public <J> JavaType<J> resolveDescriptor(
			Type javaType,
			BiFunction<Type, TypeConfiguration, MutabilityPlan<?>> mutabilityPlanCreator) {
		return resolveDescriptor(
				javaType,
				() -> {
					if ( javaType instanceof ParameterizedType ) {
						final ParameterizedType parameterizedType = (ParameterizedType) javaType;
						final JavaType<J> rawType = findDescriptor( parameterizedType.getRawType() );
						if ( rawType != null ) {
							return rawType.createJavaType( parameterizedType, typeConfiguration );
						}
					}
					final Type elementJavaType;
					JavaType<J> elementTypeDescriptor;
					if ( javaType instanceof Class<?> && ( (Class<?>) javaType ).isArray() ) {
						elementJavaType = ( (Class<?>) javaType ).getComponentType();
						elementTypeDescriptor = findDescriptor( elementJavaType );
					}
					else {
						elementJavaType = javaType;
						elementTypeDescriptor = null;
					}
					if ( elementTypeDescriptor == null ) {
						//noinspection unchecked
						elementTypeDescriptor = RegistryHelper.INSTANCE.createTypeDescriptor(
								elementJavaType,
								() -> (MutabilityPlan<J>) mutabilityPlanCreator.apply( elementJavaType, typeConfiguration ),
								typeConfiguration
						);
					}
					if ( javaType != elementJavaType ) {
						//noinspection unchecked
						return (JavaType<J>) new ArrayJavaType<>( elementTypeDescriptor );
					}
					return elementTypeDescriptor;
				}
		);
	}

	public <J> JavaType<J> resolveManagedTypeDescriptor(Type javaType) {
		return resolveManagedTypeDescriptor( javaType, false );
	}

	public <J> JavaType<J> resolveEntityTypeDescriptor(Type javaType) {
		return resolveManagedTypeDescriptor( javaType, true );
	}

	@SuppressWarnings("unchecked")
	private <J> JavaType<J> resolveManagedTypeDescriptor(Type javaType, boolean entity) {
		return resolveDescriptor(
				javaType,
				() -> {
					final Class<J> javaTypeClass;
					if ( javaType instanceof Class<?> ) {
						javaTypeClass = (Class<J>) javaType;
					}
					else {
						final ParameterizedType parameterizedType = (ParameterizedType) javaType;
						javaTypeClass = (Class<J>) parameterizedType.getRawType();
					}

					final MutabilityPlan<J> determinedPlan = RegistryHelper.INSTANCE.determineMutabilityPlan(
							javaType,
							typeConfiguration
					);
					final MutabilityPlan<J> mutabilityPlan = (determinedPlan != null)
							? determinedPlan
							: (MutabilityPlan<J>) MutableMutabilityPlan.INSTANCE;

					return entity
							? new EntityJavaType<>( javaTypeClass, mutabilityPlan )
							: new JavaTypeBasicAdaptor<>( javaTypeClass, mutabilityPlan );
				}
		);
	}
}
