/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.SourceModelBuildingContext;

import jakarta.persistence.Converter;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ConverterJpaAnnotation implements Converter {

	private boolean autoApply;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ConverterJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.autoApply = false;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ConverterJpaAnnotation(Converter annotation, SourceModelBuildingContext modelContext) {
		this.autoApply = annotation.autoApply();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ConverterJpaAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.autoApply = (boolean) attributeValues.get( "autoApply" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Converter.class;
	}

	@Override
	public boolean autoApply() {
		return autoApply;
	}

	public void autoApply(boolean value) {
		this.autoApply = value;
	}


}
