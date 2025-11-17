/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeRegistration;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class JdbcTypeRegistrationAnnotation implements JdbcTypeRegistration {
	private java.lang.Class<? extends org.hibernate.type.descriptor.jdbc.JdbcType> value;
	private int registrationCode;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public JdbcTypeRegistrationAnnotation(ModelsContext modelContext) {
		this.registrationCode = -2147483648;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public JdbcTypeRegistrationAnnotation(JdbcTypeRegistration annotation, ModelsContext modelContext) {
		this.value = annotation.value();
		this.registrationCode = annotation.registrationCode();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public JdbcTypeRegistrationAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (Class<? extends org.hibernate.type.descriptor.jdbc.JdbcType>) attributeValues.get( "value" );
		this.registrationCode = (int) attributeValues.get( "registrationCode" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return JdbcTypeRegistration.class;
	}

	@Override
	public java.lang.Class<? extends org.hibernate.type.descriptor.jdbc.JdbcType> value() {
		return value;
	}

	public void value(java.lang.Class<? extends org.hibernate.type.descriptor.jdbc.JdbcType> value) {
		this.value = value;
	}


	@Override
	public int registrationCode() {
		return registrationCode;
	}

	public void registrationCode(int value) {
		this.registrationCode = value;
	}


}
