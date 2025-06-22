/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.Convert;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ConvertJpaAnnotation implements Convert {

	private java.lang.Class<? extends jakarta.persistence.AttributeConverter> converter;
	private String attributeName;
	private boolean disableConversion;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ConvertJpaAnnotation(ModelsContext modelContext) {
		this.converter = jakarta.persistence.AttributeConverter.class;
		this.attributeName = "";
		this.disableConversion = false;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ConvertJpaAnnotation(Convert annotation, ModelsContext modelContext) {
		this.converter = annotation.converter();
		this.attributeName = annotation.attributeName();
		this.disableConversion = annotation.disableConversion();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ConvertJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.converter = (Class<? extends jakarta.persistence.AttributeConverter>) attributeValues.get( "converter" );
		this.attributeName = (String) attributeValues.get( "attributeName" );
		this.disableConversion = (boolean) attributeValues.get( "disableConversion" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Convert.class;
	}

	@Override
	public java.lang.Class<? extends jakarta.persistence.AttributeConverter> converter() {
		return converter;
	}

	public void converter(java.lang.Class<? extends jakarta.persistence.AttributeConverter> value) {
		this.converter = value;
	}


	@Override
	public String attributeName() {
		return attributeName;
	}

	public void attributeName(String value) {
		this.attributeName = value;
	}


	@Override
	public boolean disableConversion() {
		return disableConversion;
	}

	public void disableConversion(boolean value) {
		this.disableConversion = value;
	}


}
