/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.AnyKeyJdbcType;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.type.descriptor.jdbc.JdbcType;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AnyKeyJdbcTypeAnnotation implements AnyKeyJdbcType {
	private Class<? extends JdbcType> value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AnyKeyJdbcTypeAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AnyKeyJdbcTypeAnnotation(AnyKeyJdbcType annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AnyKeyJdbcTypeAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.value = (Class<? extends JdbcType>) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return AnyKeyJdbcType.class;
	}

	@Override
	public Class<? extends JdbcType> value() {
		return value;
	}

	public void value(Class<? extends JdbcType> value) {
		this.value = value;
	}


}
