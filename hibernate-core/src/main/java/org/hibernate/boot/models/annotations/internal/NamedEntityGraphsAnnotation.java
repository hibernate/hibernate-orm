/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import org.hibernate.annotations.NamedEntityGraph;
import org.hibernate.annotations.NamedEntityGraphs;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import java.lang.annotation.Annotation;
import java.util.Map;

import static org.hibernate.boot.models.HibernateAnnotations.NAMED_ENTITY_GRAPHS;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
public class NamedEntityGraphsAnnotation implements NamedEntityGraphs, RepeatableContainer<NamedEntityGraph> {
	private NamedEntityGraph[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public NamedEntityGraphsAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NamedEntityGraphsAnnotation(NamedEntityGraphs annotation, ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, NAMED_ENTITY_GRAPHS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NamedEntityGraphsAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (NamedEntityGraph[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return NamedEntityGraphs.class;
	}

	@Override
	public NamedEntityGraph[] value() {
		return value;
	}

	public void value(NamedEntityGraph[] value) {
		this.value = value;
	}
}
