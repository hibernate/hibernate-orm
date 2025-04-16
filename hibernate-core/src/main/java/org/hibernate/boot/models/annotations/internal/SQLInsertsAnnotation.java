/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLInserts;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SQLInsertsAnnotation implements SQLInserts, RepeatableContainer<SQLInsert> {
	private org.hibernate.annotations.SQLInsert[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SQLInsertsAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SQLInsertsAnnotation(SQLInserts annotation, ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, HibernateAnnotations.SQL_INSERTS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SQLInsertsAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (SQLInsert[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SQLInserts.class;
	}

	@Override
	public org.hibernate.annotations.SQLInsert[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.SQLInsert[] value) {
		this.value = value;
	}


}
