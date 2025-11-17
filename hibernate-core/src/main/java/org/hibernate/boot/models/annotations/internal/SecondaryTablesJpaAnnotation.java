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

import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SecondaryTablesJpaAnnotation implements SecondaryTables, RepeatableContainer<SecondaryTable> {
	private jakarta.persistence.SecondaryTable[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SecondaryTablesJpaAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SecondaryTablesJpaAnnotation(SecondaryTables annotation, ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, JpaAnnotations.SECONDARY_TABLES, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SecondaryTablesJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (SecondaryTable[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SecondaryTables.class;
	}

	@Override
	public jakarta.persistence.SecondaryTable[] value() {
		return value;
	}

	public void value(jakarta.persistence.SecondaryTable[] value) {
		this.value = value;
	}


}
