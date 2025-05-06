/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.NamedAttributeNode;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NamedAttributeNodeJpaAnnotation implements NamedAttributeNode {
	private String value;
	private String subgraph;
	private String keySubgraph;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public NamedAttributeNodeJpaAnnotation(ModelsContext modelContext) {
		this.subgraph = "";
		this.keySubgraph = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NamedAttributeNodeJpaAnnotation(NamedAttributeNode annotation, ModelsContext modelContext) {
		this.value = annotation.value();
		this.subgraph = annotation.subgraph();
		this.keySubgraph = annotation.keySubgraph();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NamedAttributeNodeJpaAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (String) attributeValues.get( "value" );
		this.subgraph = (String) attributeValues.get( "subgraph" );
		this.keySubgraph = (String) attributeValues.get( "keySubgraph" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return NamedAttributeNode.class;
	}

	@Override
	public String value() {
		return value;
	}

	public void value(String value) {
		this.value = value;
	}


	@Override
	public String subgraph() {
		return subgraph;
	}

	public void subgraph(String value) {
		this.subgraph = value;
	}


	@Override
	public String keySubgraph() {
		return keySubgraph;
	}

	public void keySubgraph(String value) {
		this.keySubgraph = value;
	}


}
