/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.ColumnTransformers;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ColumnTransformersAnnotation implements ColumnTransformers, RepeatableContainer<ColumnTransformer> {
	private org.hibernate.annotations.ColumnTransformer[] value;

	public ColumnTransformersAnnotation(ModelsContext modelContext) {
	}

	public ColumnTransformersAnnotation(ColumnTransformers annotation, ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, HibernateAnnotations.COLUMN_TRANSFORMERS, "value", modelContext );
	}

	public ColumnTransformersAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (ColumnTransformer[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ColumnTransformers.class;
	}

	@Override
	public org.hibernate.annotations.ColumnTransformer[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.ColumnTransformer[] value) {
		this.value = value;
	}


}
