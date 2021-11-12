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
import java.util.function.Supplier;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;

import org.jboss.logging.Logger;

/**
 * Basically a map from {@link Class} -> {@link JavaType}
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 *
 * @since 5.3
 */
public class JavaTypeRegistry implements JavaTypeDescriptorBaseline.BaselineTarget, Serializable {
	private static final Logger log = Logger.getLogger( JavaTypeRegistry.class );

	private final TypeConfiguration typeConfiguration;
	private final ConcurrentHashMap<Type, JavaType<?>> descriptorsByType = new ConcurrentHashMap<>();

	public JavaTypeRegistry(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
		JavaTypeDescriptorBaseline.prime( this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// baseline descriptors

	@Override
	public void addBaselineDescriptor(JavaType<?> descriptor) {
		if ( descriptor.getJavaType() == null ) {
			throw new IllegalStateException( "Illegal to add BasicJavaTypeDescriptor with null Java type" );
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
			// would be nice to make the JavaTypeDescriptor for an entity, e.g., aware of the the TypeConfiguration
			( (TypeConfigurationAware) descriptor ).setTypeConfiguration( typeConfiguration );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// descriptor access

	public <T> JavaType<T> getDescriptor(Type javaType) {
		return resolveDescriptor( javaType );
//		return RegistryHelper.INSTANCE.resolveDescriptor(
//				descriptorsByClass,
//				javaType,
//				() -> {
//					log.debugf(
//							"Could not find matching scoped JavaTypeDescriptor for requested Java class [%s]; " +
//									"falling back to static registry",
//							javaType.getName()
//					);
//
//					if ( Serializable.class.isAssignableFrom( javaType ) ) {
//						return new SerializableTypeDescriptor( javaType );
//					}
//
//					if ( !AttributeConverter.class.isAssignableFrom( javaType ) ) {
//						log.debugf(
//								"Could not find matching JavaTypeDescriptor for requested Java class [%s]; using fallback.  " +
//										"This means Hibernate does not know how to perform certain basic operations in relation to this Java type." +
//										"",
//								javaType.getName()
//						);
//						checkEqualsAndHashCode( javaType );
//					}
//
//					return new FallbackJavaTypeDescriptor<>( javaType );
//				}
//		);
	}

	public void addDescriptor(JavaType<?> descriptor) {
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

	public <J> JavaType<J> findDescriptor(Type javaType) {
		return (JavaType<J>) descriptorsByType.get( javaType );
	}

	public <J> JavaType<J> resolveDescriptor(Type javaType, Supplier<JavaType<J>> creator) {
		final JavaType<?> cached = descriptorsByType.get( javaType );
		if ( cached != null ) {
			//noinspection unchecked
			return (JavaType<J>) cached;
		}

		final JavaType<J> created = creator.get();
		descriptorsByType.put( javaType, created );
		return created;
	}

	public <J> JavaType<J> resolveDescriptor(Type javaType) {
		return resolveDescriptor(
				javaType,
				() -> {
					final Class<?> javaTypeClass;
					if ( javaType instanceof Class<?> ) {
						javaTypeClass = (Class<?>) javaType;
					}
					else {
						final ParameterizedType parameterizedType = (ParameterizedType) javaType;
						javaTypeClass = (Class<?>) parameterizedType.getRawType();
					}

					return RegistryHelper.INSTANCE.createTypeDescriptor(
							javaTypeClass,
							() -> {
								final MutabilityPlan<J> determinedPlan = RegistryHelper.INSTANCE.determineMutabilityPlan( javaType, typeConfiguration );
								if ( determinedPlan != null ) {
									return determinedPlan;
								}

								//noinspection unchecked
								return (MutabilityPlan<J>) MutableMutabilityPlan.INSTANCE;

							},
							typeConfiguration
					);
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
					final MutabilityPlan<J> mutabilityPlan;
					final MutabilityPlan<J> determinedPlan = RegistryHelper.INSTANCE.determineMutabilityPlan(
							javaType,
							typeConfiguration
					);
					if ( determinedPlan != null ) {
						mutabilityPlan = determinedPlan;
					}
					else {
						mutabilityPlan = (MutabilityPlan<J>) MutableMutabilityPlan.INSTANCE;
					}
					return entity ? new EntityJavaTypeDescriptor<>( javaTypeClass, mutabilityPlan )
							: new JavaTypeDescriptorBasicAdaptor<>( javaTypeClass, mutabilityPlan );
				}
		);
	}

	public JavaType<?> resolveDynamicEntityDescriptor(String typeName) {
		return new DynamicModelJtd();
	}

}
