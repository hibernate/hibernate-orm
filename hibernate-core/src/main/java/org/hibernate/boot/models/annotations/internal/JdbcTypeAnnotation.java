/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.JdbcType;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class JdbcTypeAnnotation implements JdbcType {
	private java.lang.Class<? extends org.hibernate.type.descriptor.jdbc.JdbcType> value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public JdbcTypeAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public JdbcTypeAnnotation(JdbcType annotation, ModelsContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public JdbcTypeAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (Class<? extends org.hibernate.type.descriptor.jdbc.JdbcType>) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return JdbcType.class;
	}

	@Override
	public java.lang.Class<? extends org.hibernate.type.descriptor.jdbc.JdbcType> value() {
		return value;
	}

	public void value(java.lang.Class<? extends org.hibernate.type.descriptor.jdbc.JdbcType> value) {
		this.value = value;
	}


}
