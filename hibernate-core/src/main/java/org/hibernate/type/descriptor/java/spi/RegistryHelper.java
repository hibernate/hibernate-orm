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
import org.hibernate.type.descriptor.java.BasicJavaType;
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
		return createTypeDescriptor( determineJavaTypeClass( javaType ),
				javaTypeClass -> mutabilityPlan( javaTypeClass, fallbackMutabilityPlanResolver, typeConfiguration ) );
	}

	public <J> JavaType<J> createTypeDescriptor(
			Class<J> javaType,
			Supplier<MutabilityPlan<J>> fallbackMutabilityPlanResolver,
			TypeConfiguration typeConfiguration) {
		return createTypeDescriptor( javaType,
				javaTypeClass -> mutabilityPlan( javaTypeClass, fallbackMutabilityPlanResolver, typeConfiguration ) );
	}

	private <J> MutabilityPlan<J> mutabilityPlan(
			Class<J> javaTypeClass,
			Supplier<MutabilityPlan<J>> fallbackMutabilityPlanResolver,
			TypeConfiguration typeConfiguration) {
		var mutabilityPlan = determineMutabilityPlan( javaTypeClass, typeConfiguration );
		return mutabilityPlan == null ? fallbackMutabilityPlanResolver.get() : mutabilityPlan;
	}

	public MutabilityPlan<?> determineMutabilityPlan(Type javaType, TypeConfiguration typeConfiguration) {
		return determineMutabilityPlan( determineJavaTypeClass( javaType ), typeConfiguration );
	}

	public <J> MutabilityPlan<J> determineMutabilityPlan(Class<J> javaTypeClass, TypeConfiguration typeConfiguration) {
		if ( javaTypeClass.isAnnotationPresent( Immutable.class ) ) {
			return ImmutableMutabilityPlan.instance();
		}

		if ( javaTypeClass.isAnnotationPresent( Mutability.class ) ) {
			final var annotation = javaTypeClass.getAnnotation( Mutability.class );
			return typeConfiguration.createMutabilityPlan( annotation.value() );
		}

		if ( javaTypeClass.isEnum() || javaTypeClass.isPrimitive() || javaTypeClass.isRecord() ) {
			return ImmutableMutabilityPlan.instance();
		}

		if ( Serializable.class.isAssignableFrom( javaTypeClass ) ) {
			return (MutabilityPlan<J>) SerializableJavaType.SerializableMutabilityPlan.INSTANCE;
		}
		return null;
	}

	private <J> BasicJavaType<J> createTypeDescriptor(
			Class<J> javaTypeClass,
			Function<Class<J>, MutabilityPlan<J>> mutabilityPlanResolver) {
		if ( javaTypeClass.isEnum() ) {
			// enums are unequivocally immutable
			//noinspection rawtypes, unchecked
			return new EnumJavaType( javaTypeClass );
		}

		final var plan = mutabilityPlanResolver.apply( javaTypeClass );

		if ( Serializable.class.isAssignableFrom( javaTypeClass ) ) {
			//noinspection rawtypes, unchecked
			return new SerializableJavaType( javaTypeClass, plan );
		}

		return new UnknownBasicJavaType<>( javaTypeClass, plan );
	}

	private <J> Class<J> determineJavaTypeClass(Type javaType) {
		return ReflectHelper.getClass( javaType );
	}
}
