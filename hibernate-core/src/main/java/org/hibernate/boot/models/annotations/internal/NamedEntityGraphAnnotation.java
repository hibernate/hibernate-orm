/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import org.hibernate.annotations.NamedEntityGraph;
import org.hibernate.models.spi.ModelsContext;

import java.lang.annotation.Annotation;
import java.util.Map;


/**
 * @author Steve Ebersole
 */
public class NamedEntityGraphAnnotation implements NamedEntityGraph {
	private String name;
	private String graph;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public NamedEntityGraphAnnotation(ModelsContext modelContext) {
		name = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NamedEntityGraphAnnotation(NamedEntityGraph annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.graph = annotation.graph();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NamedEntityGraphAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.graph = (String) attributeValues.get( "graph" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return NamedEntityGraph.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String name) {
		this.name = name;
	}

	@Override
	public String graph() {
		return graph;
	}

	public void graph(String graph) {
		this.graph = graph;
	}
}
