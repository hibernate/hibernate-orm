/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.TableGenerator;
import jakarta.persistence.TableGenerators;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class TableGeneratorsJpaAnnotation implements TableGenerators, RepeatableContainer<TableGenerator> {
	private TableGenerator[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public TableGeneratorsJpaAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public TableGeneratorsJpaAnnotation(TableGenerators annotation, ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, JpaAnnotations.TABLE_GENERATORS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public TableGeneratorsJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (TableGenerator[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return TableGenerators.class;
	}

	@Override
	public TableGenerator[] value() {
		return value;
	}

	public void value(TableGenerator[] value) {
		this.value = value;
	}


}
