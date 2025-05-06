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


import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class JoinColumnsJpaAnnotation implements JoinColumns, RepeatableContainer<JoinColumn> {
	private jakarta.persistence.JoinColumn[] value;
	private jakarta.persistence.ForeignKey foreignKey;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public JoinColumnsJpaAnnotation(ModelsContext modelContext) {
		this.foreignKey = JpaAnnotations.FOREIGN_KEY.createUsage( modelContext );
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public JoinColumnsJpaAnnotation(JoinColumns annotation, ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, JpaAnnotations.JOIN_COLUMNS, "value", modelContext );
		this.foreignKey = extractJdkValue( annotation, JpaAnnotations.JOIN_COLUMNS, "foreignKey", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public JoinColumnsJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (JoinColumn[]) attributeValues.get( "value" );
		this.foreignKey = (jakarta.persistence.ForeignKey) attributeValues.get( "foreignKey" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return JoinColumns.class;
	}

	@Override
	public jakarta.persistence.JoinColumn[] value() {
		return value;
	}

	public void value(jakarta.persistence.JoinColumn[] value) {
		this.value = value;
	}


	@Override
	public jakarta.persistence.ForeignKey foreignKey() {
		return foreignKey;
	}

	public void foreignKey(jakarta.persistence.ForeignKey value) {
		this.foreignKey = value;
	}


}
