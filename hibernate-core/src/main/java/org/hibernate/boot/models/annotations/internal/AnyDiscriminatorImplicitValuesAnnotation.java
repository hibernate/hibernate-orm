/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import org.hibernate.annotations.AnyDiscriminatorImplicitValues;
import org.hibernate.metamodel.spi.ImplicitDiscriminatorStrategy;
import org.hibernate.models.spi.ModelsContext;

import java.lang.annotation.Annotation;
import java.util.Map;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AnyDiscriminatorImplicitValuesAnnotation implements AnyDiscriminatorImplicitValues {
	private AnyDiscriminatorImplicitValues.Strategy value;
	private Class<? extends ImplicitDiscriminatorStrategy> implementation;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AnyDiscriminatorImplicitValuesAnnotation(ModelsContext modelContext) {
		this.value = Strategy.CUSTOM;
		this.implementation = ImplicitDiscriminatorStrategy.class;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AnyDiscriminatorImplicitValuesAnnotation(AnyDiscriminatorImplicitValues annotation, ModelsContext modelContext) {
		this.value = annotation.value();
		this.implementation = annotation.implementation();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AnyDiscriminatorImplicitValuesAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (Strategy) attributeValues.get( "value" );
		//noinspection unchecked
		this.implementation = (Class<? extends ImplicitDiscriminatorStrategy>) attributeValues.get( "implementation" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return AnyDiscriminatorImplicitValues.class;
	}


	@Override
	public Strategy value() {
		return value;
	}

	public void value(Strategy value) {
		this.value = value;
	}


	@Override
	public Class<? extends ImplicitDiscriminatorStrategy> implementation() {
		return implementation;
	}

	public void implementation(Class<? extends ImplicitDiscriminatorStrategy> implementation) {
		this.implementation = implementation;
	}
}
