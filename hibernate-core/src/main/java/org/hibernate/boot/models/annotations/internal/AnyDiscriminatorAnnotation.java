/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.metamodel.spi.ImplicitDiscriminatorStrategy;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AnyDiscriminatorAnnotation implements AnyDiscriminator {
	private jakarta.persistence.DiscriminatorType value;
	private Class<? extends ImplicitDiscriminatorStrategy> implicitValueStrategy;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AnyDiscriminatorAnnotation(SourceModelBuildingContext modelContext) {
		this.value = jakarta.persistence.DiscriminatorType.STRING;
		this.implicitValueStrategy = ImplicitDiscriminatorStrategy.class;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AnyDiscriminatorAnnotation(AnyDiscriminator annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
		this.implicitValueStrategy = annotation.implicitValueStrategy();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AnyDiscriminatorAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.value = (jakarta.persistence.DiscriminatorType) attributeValues.get( "value" );
		//noinspection unchecked
		this.implicitValueStrategy = (Class<? extends ImplicitDiscriminatorStrategy>) attributeValues.get( "implicitValueStrategy" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return AnyDiscriminator.class;
	}

	@Override
	public jakarta.persistence.DiscriminatorType value() {
		return value;
	}

	public void value(jakarta.persistence.DiscriminatorType value) {
		this.value = value;
	}

	@Override
	public Class<? extends ImplicitDiscriminatorStrategy> implicitValueStrategy() {
		return implicitValueStrategy;
	}

	public void implicitValueStrategy(Class<? extends ImplicitDiscriminatorStrategy> implicitValueStrategy) {
		this.implicitValueStrategy = implicitValueStrategy;
	}
}
