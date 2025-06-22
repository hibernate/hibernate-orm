/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.NamedSubgraph;

import static org.hibernate.boot.models.JpaAnnotations.NAMED_SUBGRAPH;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NamedSubgraphJpaAnnotation implements NamedSubgraph {
	private String name;
	private java.lang.Class<?> type;
	private jakarta.persistence.NamedAttributeNode[] attributeNodes;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public NamedSubgraphJpaAnnotation(ModelsContext modelContext) {
		this.type = void.class;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NamedSubgraphJpaAnnotation(NamedSubgraph annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.type = annotation.type();
		this.attributeNodes = extractJdkValue( annotation, NAMED_SUBGRAPH, "attributeNodes", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NamedSubgraphJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.type = (Class<?>) attributeValues.get( "type" );
		this.attributeNodes = (jakarta.persistence.NamedAttributeNode[]) attributeValues.get( "attributeNodes" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return NamedSubgraph.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public java.lang.Class<?> type() {
		return type;
	}

	public void type(java.lang.Class<?> value) {
		this.type = value;
	}


	@Override
	public jakarta.persistence.NamedAttributeNode[] attributeNodes() {
		return attributeNodes;
	}

	public void attributeNodes(jakarta.persistence.NamedAttributeNode[] value) {
		this.attributeNodes = value;
	}


}
