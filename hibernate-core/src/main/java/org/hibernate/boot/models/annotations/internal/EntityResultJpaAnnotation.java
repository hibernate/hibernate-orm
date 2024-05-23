/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import jakarta.persistence.EntityResult;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

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
	public EntityResultJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.lockMode = jakarta.persistence.LockModeType.OPTIMISTIC;
		this.fields = new jakarta.persistence.FieldResult[0];
		this.discriminatorColumn = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public EntityResultJpaAnnotation(EntityResult annotation, SourceModelBuildingContext modelContext) {
		this.entityClass = extractJdkValue( annotation, JpaAnnotations.ENTITY_RESULT, "entityClass", modelContext );
		this.lockMode = extractJdkValue( annotation, JpaAnnotations.ENTITY_RESULT, "lockMode", modelContext );
		this.fields = extractJdkValue( annotation, JpaAnnotations.ENTITY_RESULT, "fields", modelContext );
		this.discriminatorColumn = extractJdkValue(
				annotation,
				JpaAnnotations.ENTITY_RESULT,
				"discriminatorColumn",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public EntityResultJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.entityClass = extractJandexValue( annotation, JpaAnnotations.ENTITY_RESULT, "entityClass", modelContext );
		this.lockMode = extractJandexValue( annotation, JpaAnnotations.ENTITY_RESULT, "lockMode", modelContext );
		this.fields = extractJandexValue( annotation, JpaAnnotations.ENTITY_RESULT, "fields", modelContext );
		this.discriminatorColumn = extractJandexValue(
				annotation,
				JpaAnnotations.ENTITY_RESULT,
				"discriminatorColumn",
				modelContext
		);
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
