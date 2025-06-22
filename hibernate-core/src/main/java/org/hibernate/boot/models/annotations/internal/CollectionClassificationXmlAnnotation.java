/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.internal.CollectionClassification;
import org.hibernate.boot.internal.LimitedCollectionClassification;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CollectionClassificationXmlAnnotation implements CollectionClassification {
	private LimitedCollectionClassification value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CollectionClassificationXmlAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CollectionClassificationXmlAnnotation(
			CollectionClassification annotation,
			ModelsContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CollectionClassificationXmlAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (LimitedCollectionClassification) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return CollectionClassification.class;
	}

	@Override
	public LimitedCollectionClassification value() {
		return value;
	}

	public void value(LimitedCollectionClassification value) {
		this.value = value;
	}


}
