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

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Mutability;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.SerializableTypeDescriptor;
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

	@SuppressWarnings("unchecked")
	public <J> JavaTypeDescriptor<J> createTypeDescriptor(Type javaType, TypeConfiguration typeConfiguration) {
		return createTypeDescriptor(
				javaType,
				(javaTypeClass) -> {
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

					return ImmutableMutabilityPlan.INSTANCE;
				}
		);
	}

	@SuppressWarnings("unchecked")
	public <J> JavaTypeDescriptor<J> createTypeDescriptor(
			Type javaType,
			Function<Class<J>,MutabilityPlan<J>> mutabilityPlanResolver) {
		final Class<J> javaTypeClass;
		if ( javaType instanceof Class<?> ) {
			javaTypeClass = (Class<J>) javaType;
		}
		else {
			final ParameterizedType parameterizedType = (ParameterizedType) javaType;
			javaTypeClass = (Class<J>) parameterizedType.getRawType();
		}

		if ( javaTypeClass.isEnum() ) {
			// enums are unequivocally immutable
			//noinspection rawtypes
			return new EnumJavaTypeDescriptor( javaTypeClass );
		}

		final MutabilityPlan<J> plan = mutabilityPlanResolver.apply( javaTypeClass );

		if ( Serializable.class.isAssignableFrom( javaTypeClass ) ) {
			//noinspection rawtypes
			return new SerializableTypeDescriptor( javaTypeClass, plan );
		}

		return new JavaTypeDescriptorBasicAdaptor<>( javaTypeClass, plan );
	}
}
