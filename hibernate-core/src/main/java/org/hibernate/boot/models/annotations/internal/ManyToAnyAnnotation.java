/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.ManyToAny;
import org.hibernate.boot.models.annotations.spi.AttributeMarker;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ManyToAnyAnnotation implements ManyToAny, AttributeMarker, AttributeMarker.Fetchable {
	private jakarta.persistence.FetchType fetch;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ManyToAnyAnnotation(ModelsContext modelContext) {
		this.fetch = jakarta.persistence.FetchType.EAGER;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ManyToAnyAnnotation(ManyToAny annotation, ModelsContext modelContext) {
		this.fetch = annotation.fetch();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ManyToAnyAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.fetch = (jakarta.persistence.FetchType) attributeValues.get( "fetch" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ManyToAny.class;
	}

	@Override
	public jakarta.persistence.FetchType fetch() {
		return fetch;
	}

	public void fetch(jakarta.persistence.FetchType value) {
		this.fetch = value;
	}


}
