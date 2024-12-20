/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.EmbeddableInstantiator;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class EmbeddableInstantiatorAnnotation implements EmbeddableInstantiator {
	private java.lang.Class<? extends org.hibernate.metamodel.spi.EmbeddableInstantiator> value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public EmbeddableInstantiatorAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public EmbeddableInstantiatorAnnotation(
			EmbeddableInstantiator annotation,
			SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public EmbeddableInstantiatorAnnotation(
			Map<String, Object> attributeValues,
			SourceModelBuildingContext modelContext) {
		this.value = (Class<? extends org.hibernate.metamodel.spi.EmbeddableInstantiator>) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return EmbeddableInstantiator.class;
	}

	@Override
	public java.lang.Class<? extends org.hibernate.metamodel.spi.EmbeddableInstantiator> value() {
		return value;
	}

	public void value(java.lang.Class<? extends org.hibernate.metamodel.spi.EmbeddableInstantiator> value) {
		this.value = value;
	}


}
