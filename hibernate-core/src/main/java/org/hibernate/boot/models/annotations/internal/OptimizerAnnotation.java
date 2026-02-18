/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Optimizer;
import org.hibernate.id.enhanced.StandardOptimizerDescriptor;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class OptimizerAnnotation implements Optimizer {
	private StandardOptimizerDescriptor value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OptimizerAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OptimizerAnnotation(Optimizer annotation, ModelsContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OptimizerAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (StandardOptimizerDescriptor) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Optimizer.class;
	}

	@Override
	public StandardOptimizerDescriptor value() {
		return value;
	}

	public void value(StandardOptimizerDescriptor value) {
		this.value = value;
	}
}
