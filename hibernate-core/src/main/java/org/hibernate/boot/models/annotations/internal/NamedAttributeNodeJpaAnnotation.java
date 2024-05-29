/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import jakarta.persistence.NamedAttributeNode;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NamedAttributeNodeJpaAnnotation implements NamedAttributeNode {
	private String value;
	private String subgraph;
	private String keySubgraph;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public NamedAttributeNodeJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.subgraph = "";
		this.keySubgraph = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NamedAttributeNodeJpaAnnotation(NamedAttributeNode annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
		this.subgraph = annotation.subgraph();
		this.keySubgraph = annotation.keySubgraph();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NamedAttributeNodeJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJandexValue( annotation, JpaAnnotations.NAMED_ATTRIBUTE_NODE, "value", modelContext );
		this.subgraph = extractJandexValue( annotation, JpaAnnotations.NAMED_ATTRIBUTE_NODE, "subgraph", modelContext );
		this.keySubgraph = extractJandexValue(
				annotation,
				JpaAnnotations.NAMED_ATTRIBUTE_NODE,
				"keySubgraph",
				modelContext
		);
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
