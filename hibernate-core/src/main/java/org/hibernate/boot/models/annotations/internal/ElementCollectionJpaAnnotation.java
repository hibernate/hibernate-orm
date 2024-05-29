/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.spi.AttributeMarker;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import jakarta.persistence.ElementCollection;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ElementCollectionJpaAnnotation implements ElementCollection, AttributeMarker.Fetchable {
	private java.lang.Class<?> targetClass;
	private jakarta.persistence.FetchType fetch;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ElementCollectionJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.targetClass = void.class;
		this.fetch = jakarta.persistence.FetchType.LAZY;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ElementCollectionJpaAnnotation(ElementCollection annotation, SourceModelBuildingContext modelContext) {
		this.targetClass = annotation.targetClass();
		this.fetch = annotation.fetch();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ElementCollectionJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.targetClass = extractJandexValue(
				annotation,
				JpaAnnotations.ELEMENT_COLLECTION,
				"targetClass",
				modelContext
		);
		this.fetch = extractJandexValue( annotation, JpaAnnotations.ELEMENT_COLLECTION, "fetch", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ElementCollection.class;
	}

	@Override
	public java.lang.Class<?> targetClass() {
		return targetClass;
	}

	public void targetClass(java.lang.Class<?> value) {
		this.targetClass = value;
	}


	@Override
	public jakarta.persistence.FetchType fetch() {
		return fetch;
	}

	public void fetch(jakarta.persistence.FetchType value) {
		this.fetch = value;
	}


}
