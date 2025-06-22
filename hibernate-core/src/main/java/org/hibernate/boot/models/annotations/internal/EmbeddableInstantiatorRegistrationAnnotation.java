/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.EmbeddableInstantiatorRegistration;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class EmbeddableInstantiatorRegistrationAnnotation implements EmbeddableInstantiatorRegistration {
	private java.lang.Class<?> embeddableClass;
	private java.lang.Class<? extends org.hibernate.metamodel.spi.EmbeddableInstantiator> instantiator;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public EmbeddableInstantiatorRegistrationAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public EmbeddableInstantiatorRegistrationAnnotation(
			EmbeddableInstantiatorRegistration annotation,
			ModelsContext modelContext) {
		this.embeddableClass = annotation.embeddableClass();
		this.instantiator = annotation.instantiator();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public EmbeddableInstantiatorRegistrationAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.embeddableClass = (Class<?>) attributeValues.get( "embeddableClass" );
		this.instantiator = (Class<? extends org.hibernate.metamodel.spi.EmbeddableInstantiator>) attributeValues
				.get( "instantiator" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return EmbeddableInstantiatorRegistration.class;
	}

	@Override
	public java.lang.Class<?> embeddableClass() {
		return embeddableClass;
	}

	public void embeddableClass(java.lang.Class<?> value) {
		this.embeddableClass = value;
	}


	@Override
	public java.lang.Class<? extends org.hibernate.metamodel.spi.EmbeddableInstantiator> instantiator() {
		return instantiator;
	}

	public void instantiator(java.lang.Class<? extends org.hibernate.metamodel.spi.EmbeddableInstantiator> value) {
		this.instantiator = value;
	}


}
