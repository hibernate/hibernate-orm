/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.MapKeyJdbcTypeCode;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class MapKeyJdbcTypeCodeAnnotation implements MapKeyJdbcTypeCode {
	private int value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public MapKeyJdbcTypeCodeAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public MapKeyJdbcTypeCodeAnnotation(MapKeyJdbcTypeCode annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue( annotation, HibernateAnnotations.MAP_KEY_JDBC_TYPE_CODE, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public MapKeyJdbcTypeCodeAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJandexValue(
				annotation,
				HibernateAnnotations.MAP_KEY_JDBC_TYPE_CODE,
				"value",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return MapKeyJdbcTypeCode.class;
	}

	@Override
	public int value() {
		return value;
	}

	public void value(int value) {
		this.value = value;
	}


}
