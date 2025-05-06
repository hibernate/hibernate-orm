/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.EmbeddableInstantiatorRegistration;
import org.hibernate.annotations.EmbeddableInstantiatorRegistrations;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class EmbeddableInstantiatorRegistrationsAnnotation
		implements EmbeddableInstantiatorRegistrations, RepeatableContainer<EmbeddableInstantiatorRegistration> {
	private org.hibernate.annotations.EmbeddableInstantiatorRegistration[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public EmbeddableInstantiatorRegistrationsAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public EmbeddableInstantiatorRegistrationsAnnotation(
			EmbeddableInstantiatorRegistrations annotation,
			ModelsContext modelContext) {
		this.value = extractJdkValue(
				annotation,
				HibernateAnnotations.EMBEDDABLE_INSTANTIATOR_REGISTRATIONS,
				"value",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public EmbeddableInstantiatorRegistrationsAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (EmbeddableInstantiatorRegistration[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return EmbeddableInstantiatorRegistrations.class;
	}

	@Override
	public org.hibernate.annotations.EmbeddableInstantiatorRegistration[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.EmbeddableInstantiatorRegistration[] value) {
		this.value = value;
	}


}
