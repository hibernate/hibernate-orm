/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.spi;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Mutability;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.SerializableJavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class RegistryHelper {

	/**
	 * Singleton access
	 */
	public static final RegistryHelper INSTANCE = new RegistryHelper();

	private RegistryHelper() {
	}

	public <J> JavaType<J> createTypeDescriptor(
			Type javaType,
			Supplier<MutabilityPlan<J>> fallbackMutabilityPlanResolver,
			TypeConfiguration typeConfiguration) {
		return createTypeDescriptor(
				javaType,
				(javaTypeClass) -> {
					MutabilityPlan<J> mutabilityPlan = determineMutabilityPlan( javaType, typeConfiguration );
					if ( mutabilityPlan == null ) {
						mutabilityPlan = fallbackMutabilityPlanResolver.get();
					}
					return mutabilityPlan;
				}
		);
	}

	@SuppressWarnings("unchecked")
	public <J> MutabilityPlan<J> determineMutabilityPlan(Type javaType, TypeConfiguration typeConfiguration) {
		final Class<J> javaTypeClass = determineJavaTypeClass( javaType );

		if ( javaTypeClass.isAnnotationPresent( Immutable.class ) ) {
			return ImmutableMutabilityPlan.INSTANCE;
		}

		if ( javaTypeClass.isAnnotationPresent( Mutability.class ) ) {
			final Mutability annotation = javaTypeClass.getAnnotation( Mutability.class );
			final Class<? extends MutabilityPlan<?>> planClass = annotation.value();
			final ManagedBeanRegistry managedBeanRegistry = typeConfiguration
					.getServiceRegistry()
					.getService( ManagedBeanRegistry.class );
			final ManagedBean<? extends MutabilityPlan<?>> planBean = managedBeanRegistry.getBean( planClass );
			return (MutabilityPlan<J>) planBean.getBeanInstance();
		}

		if ( javaTypeClass.isEnum() ) {
			return ImmutableMutabilityPlan.INSTANCE;
		}

		if ( javaTypeClass.isPrimitive() ) {
			return ImmutableMutabilityPlan.INSTANCE;
		}

		if ( Serializable.class.isAssignableFrom( javaTypeClass ) ) {
			return (MutabilityPlan<J>) SerializableJavaTypeDescriptor.SerializableMutabilityPlan.INSTANCE;
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private  <J> JavaType<J> createTypeDescriptor(
			Type javaType,
			Function<Class<J>,MutabilityPlan<J>> mutabilityPlanResolver) {
		final Class<J> javaTypeClass = determineJavaTypeClass( javaType );

		if ( javaTypeClass.isEnum() ) {
			// enums are unequivocally immutable
			//noinspection rawtypes
			return new EnumJavaTypeDescriptor( javaTypeClass );
		}

		final MutabilityPlan<J> plan = mutabilityPlanResolver.apply( javaTypeClass );

		if ( Serializable.class.isAssignableFrom( javaTypeClass ) ) {
			//noinspection rawtypes
			return new SerializableJavaTypeDescriptor( javaTypeClass, plan );
		}

		return new JavaTypeDescriptorBasicAdaptor<>( javaTypeClass, plan );
	}

	private <J> Class<J> determineJavaTypeClass(Type javaType) {
		final Class<J> javaTypeClass;
		if ( javaType instanceof Class<?> ) {
			javaTypeClass = (Class<J>) javaType;
		}
		else {
			final ParameterizedType parameterizedType = (ParameterizedType) javaType;
			javaTypeClass = (Class<J>) parameterizedType.getRawType();
		}
		return javaTypeClass;
	}
}
