/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DefaultListSemantics;
import org.hibernate.models.spi.ModelsContext;

/// @author Steve Ebersole
@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class DefaultListSemanticsAnnotation implements DefaultListSemantics {
	private Classification value;

	public DefaultListSemanticsAnnotation(ModelsContext modelContext) {
	}

	public DefaultListSemanticsAnnotation(DefaultListSemantics annotation, ModelsContext modelContext) {
		value = annotation.value();
	}

	public DefaultListSemanticsAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		value = (Classification) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DefaultListSemantics.class;
	}

	@Override
	public Classification value() {
		return value;
	}

	public void value(Classification value) {
		this.value = value;
	}
}
