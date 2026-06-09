/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.generator.Generator;
import org.hibernate.models.spi.ModelsContext;

import jakarta.annotation.Generated;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class GenericGeneratorAnnotation implements GenericGenerator {
	private Class<? extends Generator> type;
	private Parameter[] parameters;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public GenericGeneratorAnnotation(ModelsContext modelContext) {
		this.parameters = new Parameter[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public GenericGeneratorAnnotation(GenericGenerator annotation, ModelsContext modelContext) {
		this.type = annotation.type();
		this.parameters = extractJdkValue(
				annotation,
				HibernateAnnotations.GENERIC_GENERATOR,
				"parameters",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public GenericGeneratorAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.type = (Class<? extends Generator>) attributeValues.get( "type" );
		this.parameters = (Parameter[]) attributeValues.get( "parameters" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return GenericGenerator.class;
	}

	@Override
	public Class<? extends Generator> type() {
		return type;
	}

	public void type(Class<? extends Generator> value) {
		this.type = value;
	}


	@Override
	public Parameter[] parameters() {
		return parameters;
	}

	public void parameters(Parameter[] value) {
		this.parameters = value;
	}


}
