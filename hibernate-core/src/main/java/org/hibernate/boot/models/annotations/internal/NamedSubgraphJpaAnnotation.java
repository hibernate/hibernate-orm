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

import jakarta.persistence.NamedSubgraph;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
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
	public NamedSubgraphJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.type = void.class;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NamedSubgraphJpaAnnotation(NamedSubgraph annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJdkValue( annotation, JpaAnnotations.NAMED_SUBGRAPH, "name", modelContext );
		this.type = extractJdkValue( annotation, JpaAnnotations.NAMED_SUBGRAPH, "type", modelContext );
		this.attributeNodes = extractJdkValue(
				annotation,
				JpaAnnotations.NAMED_SUBGRAPH,
				"attributeNodes",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NamedSubgraphJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, JpaAnnotations.NAMED_SUBGRAPH, "name", modelContext );
		this.type = extractJandexValue( annotation, JpaAnnotations.NAMED_SUBGRAPH, "type", modelContext );
		this.attributeNodes = extractJandexValue(
				annotation,
				JpaAnnotations.NAMED_SUBGRAPH,
				"attributeNodes",
				modelContext
		);
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
