/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.internal.Target;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class TargetXmlAnnotation implements Target {
	private String value;

	public TargetXmlAnnotation(ModelsContext modelContext) {
	}

	public TargetXmlAnnotation(Target annotation, ModelsContext modelContext) {
		throw new UnsupportedOperationException( "Should only ever be sourced from XML" );
	}

	public TargetXmlAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		throw new UnsupportedOperationException( "Should only ever be sourced from XML" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Target.class;
	}

	@Override
	public String value() {
		return value;
	}

	public void value(String value) {
		this.value = value;
	}


}
