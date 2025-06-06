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

import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.MapKeyJoinColumns;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class MapKeyJoinColumnsJpaAnnotation implements MapKeyJoinColumns, RepeatableContainer<MapKeyJoinColumn> {
	private jakarta.persistence.MapKeyJoinColumn[] value;
	private jakarta.persistence.ForeignKey foreignKey;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public MapKeyJoinColumnsJpaAnnotation(ModelsContext modelContext) {
		this.foreignKey = JpaAnnotations.FOREIGN_KEY.createUsage( modelContext );
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public MapKeyJoinColumnsJpaAnnotation(MapKeyJoinColumns annotation, ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, JpaAnnotations.MAP_KEY_JOIN_COLUMNS, "value", modelContext );
		this.foreignKey = extractJdkValue(
				annotation,
				JpaAnnotations.MAP_KEY_JOIN_COLUMNS,
				"foreignKey",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public MapKeyJoinColumnsJpaAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (MapKeyJoinColumn[]) attributeValues.get( "value" );
		this.foreignKey = (jakarta.persistence.ForeignKey) attributeValues.get( "foreignKey" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return MapKeyJoinColumns.class;
	}

	@Override
	public jakarta.persistence.MapKeyJoinColumn[] value() {
		return value;
	}

	public void value(jakarta.persistence.MapKeyJoinColumn[] value) {
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
