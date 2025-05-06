/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.SQLUpdates;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SQLUpdatesAnnotation implements SQLUpdates, RepeatableContainer<SQLUpdate> {
	private org.hibernate.annotations.SQLUpdate[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SQLUpdatesAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SQLUpdatesAnnotation(SQLUpdates annotation, ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, HibernateAnnotations.SQL_UPDATES, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SQLUpdatesAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (SQLUpdate[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SQLUpdates.class;
	}

	@Override
	public org.hibernate.annotations.SQLUpdate[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.SQLUpdate[] value) {
		this.value = value;
	}


}
