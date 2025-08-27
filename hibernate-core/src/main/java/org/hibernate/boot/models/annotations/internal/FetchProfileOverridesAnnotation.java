/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.FetchProfileOverrides;
import org.hibernate.boot.models.DialectOverrideAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class FetchProfileOverridesAnnotation
		implements FetchProfileOverrides, RepeatableContainer<org.hibernate.annotations.FetchProfileOverride> {
	private org.hibernate.annotations.FetchProfileOverride[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public FetchProfileOverridesAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public FetchProfileOverridesAnnotation(FetchProfileOverrides annotation, ModelsContext modelContext) {
		this.value = extractJdkValue(
				annotation,
				DialectOverrideAnnotations.FETCH_PROFILE_OVERRIDES,
				"value",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public FetchProfileOverridesAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (org.hibernate.annotations.FetchProfileOverride[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return FetchProfileOverrides.class;
	}

	@Override
	public org.hibernate.annotations.FetchProfileOverride[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.FetchProfileOverride[] value) {
		this.value = value;
	}


}
