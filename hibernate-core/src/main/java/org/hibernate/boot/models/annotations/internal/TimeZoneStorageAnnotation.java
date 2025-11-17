/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class TimeZoneStorageAnnotation implements TimeZoneStorage {
	private org.hibernate.annotations.TimeZoneStorageType value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public TimeZoneStorageAnnotation(ModelsContext modelContext) {
		this.value = org.hibernate.annotations.TimeZoneStorageType.AUTO;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public TimeZoneStorageAnnotation(TimeZoneStorage annotation, ModelsContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public TimeZoneStorageAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (org.hibernate.annotations.TimeZoneStorageType) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return TimeZoneStorage.class;
	}

	@Override
	public org.hibernate.annotations.TimeZoneStorageType value() {
		return value;
	}

	public void value(org.hibernate.annotations.TimeZoneStorageType value) {
		this.value = value;
	}


}
