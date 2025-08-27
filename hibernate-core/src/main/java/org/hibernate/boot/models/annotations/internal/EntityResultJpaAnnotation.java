/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.EntityResult;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class EntityResultJpaAnnotation implements EntityResult {
	private java.lang.Class<?> entityClass;
	private jakarta.persistence.LockModeType lockMode;
	private jakarta.persistence.FieldResult[] fields;
	private String discriminatorColumn;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public EntityResultJpaAnnotation(ModelsContext modelContext) {
		this.lockMode = jakarta.persistence.LockModeType.OPTIMISTIC;
		this.fields = new jakarta.persistence.FieldResult[0];
		this.discriminatorColumn = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public EntityResultJpaAnnotation(EntityResult annotation, ModelsContext modelContext) {
		this.entityClass = annotation.entityClass();
		this.lockMode = annotation.lockMode();
		this.fields = annotation.fields();
		this.discriminatorColumn = annotation.discriminatorColumn();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public EntityResultJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.entityClass = (Class<?>) attributeValues.get( "entityClass" );
		this.lockMode = (jakarta.persistence.LockModeType) attributeValues.get( "lockMode" );
		this.fields = (jakarta.persistence.FieldResult[]) attributeValues.get( "fields" );
		this.discriminatorColumn = (String) attributeValues.get( "discriminatorColumn" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return EntityResult.class;
	}

	@Override
	public java.lang.Class<?> entityClass() {
		return entityClass;
	}

	public void entityClass(java.lang.Class<?> value) {
		this.entityClass = value;
	}


	@Override
	public jakarta.persistence.LockModeType lockMode() {
		return lockMode;
	}

	public void lockMode(jakarta.persistence.LockModeType value) {
		this.lockMode = value;
	}


	@Override
	public jakarta.persistence.FieldResult[] fields() {
		return fields;
	}

	public void fields(jakarta.persistence.FieldResult[] value) {
		this.fields = value;
	}


	@Override
	public String discriminatorColumn() {
		return discriminatorColumn;
	}

	public void discriminatorColumn(String value) {
		this.discriminatorColumn = value;
	}


}
