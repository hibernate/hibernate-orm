/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.type.AnyDiscriminatorValueStrategy;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AnyDiscriminatorAnnotation implements AnyDiscriminator {
	private jakarta.persistence.DiscriminatorType value;
	private AnyDiscriminatorValueStrategy valueStrategy;
	private boolean implicitEntityShortName;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AnyDiscriminatorAnnotation(SourceModelBuildingContext modelContext) {
		this.value = jakarta.persistence.DiscriminatorType.STRING;
		this.valueStrategy = AnyDiscriminatorValueStrategy.AUTO;
		this.implicitEntityShortName = false;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AnyDiscriminatorAnnotation(AnyDiscriminator annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
		this.valueStrategy = annotation.valueStrategy();
		this.implicitEntityShortName = annotation.implicitEntityShortName();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AnyDiscriminatorAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.value = (jakarta.persistence.DiscriminatorType) attributeValues.get( "value" );
		this.valueStrategy = (AnyDiscriminatorValueStrategy) attributeValues.get( "valueStrategy" );
		this.implicitEntityShortName = (boolean) attributeValues.get( "implicitEntityShortName" );
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
	public AnyDiscriminatorValueStrategy valueStrategy() {
		return valueStrategy;
	}

	public void valueStrategy(AnyDiscriminatorValueStrategy valueStrategy) {
		this.valueStrategy = valueStrategy;
	}

	@Override
	public boolean implicitEntityShortName() {
		return implicitEntityShortName;
	}

	public void implicitEntityShortName(boolean implicitEntityShortName) {
		this.implicitEntityShortName = implicitEntityShortName;
	}
}
