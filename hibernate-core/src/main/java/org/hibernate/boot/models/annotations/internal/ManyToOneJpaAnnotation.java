/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.models.annotations.spi.AttributeMarker;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.ManyToOne;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ManyToOneJpaAnnotation implements ManyToOne,
		AttributeMarker,
		AttributeMarker.Fetchable,
		AttributeMarker.Cascadeable,
		AttributeMarker.Optionalable {
	private java.lang.Class<?> targetEntity;
	private jakarta.persistence.CascadeType[] cascade;
	private jakarta.persistence.FetchType fetch;
	private boolean optional;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ManyToOneJpaAnnotation(ModelsContext modelContext) {
		this.targetEntity = void.class;
		this.cascade = new jakarta.persistence.CascadeType[0];
		this.fetch = jakarta.persistence.FetchType.EAGER;
		this.optional = true;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ManyToOneJpaAnnotation(ManyToOne annotation, ModelsContext modelContext) {
		this.targetEntity = annotation.targetEntity();
		this.cascade = annotation.cascade();
		this.fetch = annotation.fetch();
		this.optional = annotation.optional();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ManyToOneJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.targetEntity = (Class<?>) attributeValues.get( "targetEntity" );
		this.cascade = (jakarta.persistence.CascadeType[]) attributeValues.get( "cascade" );
		this.fetch = (jakarta.persistence.FetchType) attributeValues.get( "fetch" );
		this.optional = (boolean) attributeValues.get( "optional" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ManyToOne.class;
	}

	@Override
	public java.lang.Class<?> targetEntity() {
		return targetEntity;
	}

	public void targetEntity(java.lang.Class<?> value) {
		this.targetEntity = value;
	}


	@Override
	public jakarta.persistence.CascadeType[] cascade() {
		return cascade;
	}

	public void cascade(jakarta.persistence.CascadeType[] value) {
		this.cascade = value;
	}


	@Override
	public jakarta.persistence.FetchType fetch() {
		return fetch;
	}

	public void fetch(jakarta.persistence.FetchType value) {
		this.fetch = value;
	}


	@Override
	public boolean optional() {
		return optional;
	}

	public void optional(boolean value) {
		this.optional = value;
	}


}
