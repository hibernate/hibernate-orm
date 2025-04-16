/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeletes;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SQLDeletesAnnotation implements SQLDeletes, RepeatableContainer<SQLDelete> {
	private org.hibernate.annotations.SQLDelete[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SQLDeletesAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SQLDeletesAnnotation(SQLDeletes annotation, ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, HibernateAnnotations.SQL_DELETES, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SQLDeletesAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (SQLDelete[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SQLDeletes.class;
	}

	@Override
	public org.hibernate.annotations.SQLDelete[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.SQLDelete[] value) {
		this.value = value;
	}


}
