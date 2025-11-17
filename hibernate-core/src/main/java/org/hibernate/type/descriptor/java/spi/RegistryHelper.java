/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java.spi;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Mutability;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.SerializableJavaType;
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

	public <J> MutabilityPlan<J> determineMutabilityPlan(Type javaType, TypeConfiguration typeConfiguration) {
		final Class<J> javaTypeClass = determineJavaTypeClass( javaType );

		if ( javaTypeClass.isAnnotationPresent( Immutable.class ) ) {
			return ImmutableMutabilityPlan.instance();
		}

		if ( javaTypeClass.isAnnotationPresent( Mutability.class ) ) {
			final Mutability annotation = javaTypeClass.getAnnotation( Mutability.class );
			return typeConfiguration.createMutabilityPlan( annotation.value() );
		}

		if ( javaTypeClass.isEnum() || javaTypeClass.isPrimitive() || ReflectHelper.isRecord( javaTypeClass ) ) {
			return ImmutableMutabilityPlan.instance();
		}

		if ( Serializable.class.isAssignableFrom( javaTypeClass ) ) {
			return (MutabilityPlan<J>) SerializableJavaType.SerializableMutabilityPlan.INSTANCE;
		}

		return null;
	}

	private  <J> JavaType<J> createTypeDescriptor(
			Type javaType,
			Function<Class<J>,MutabilityPlan<J>> mutabilityPlanResolver) {
		final Class<J> javaTypeClass = determineJavaTypeClass( javaType );

		if ( javaTypeClass.isEnum() ) {
			// enums are unequivocally immutable
			//noinspection rawtypes, unchecked
			return new EnumJavaType( javaTypeClass );
		}

		final MutabilityPlan<J> plan = mutabilityPlanResolver.apply( javaTypeClass );

		if ( Serializable.class.isAssignableFrom( javaTypeClass ) ) {
			//noinspection rawtypes, unchecked
			return new SerializableJavaType( javaTypeClass, plan );
		}

		return new UnknownBasicJavaType<>( javaType, plan );
	}

	private <J> Class<J> determineJavaTypeClass(Type javaType) {
		return ReflectHelper.getClass( javaType );
	}
}
