/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.ConverterRegistration;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ConverterRegistrationAnnotation implements ConverterRegistration {
	private java.lang.Class<? extends jakarta.persistence.AttributeConverter<?, ?>> converter;
	private java.lang.Class<?> domainType;
	private boolean autoApply;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ConverterRegistrationAnnotation(ModelsContext modelContext) {
		this.domainType = void.class;
		this.autoApply = true;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ConverterRegistrationAnnotation(ConverterRegistration annotation, ModelsContext modelContext) {
		this.converter = annotation.converter();
		this.domainType = annotation.domainType();
		this.autoApply = annotation.autoApply();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ConverterRegistrationAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.converter = (Class<? extends jakarta.persistence.AttributeConverter<?, ?>>) attributeValues.get( "converter" );
		this.domainType = (Class<?>) attributeValues.get( "domainType" );
		this.autoApply = (boolean) attributeValues.get( "autoApply" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ConverterRegistration.class;
	}

	@Override
	public java.lang.Class<? extends jakarta.persistence.AttributeConverter<?, ?>> converter() {
		return converter;
	}

	public void converter(java.lang.Class<? extends jakarta.persistence.AttributeConverter<?, ?>> value) {
		this.converter = value;
	}


	@Override
	public java.lang.Class<?> domainType() {
		return domainType;
	}

	public void domainType(java.lang.Class<?> value) {
		this.domainType = value;
	}


	@Override
	public boolean autoApply() {
		return autoApply;
	}

	public void autoApply(boolean value) {
		this.autoApply = value;
	}


}
