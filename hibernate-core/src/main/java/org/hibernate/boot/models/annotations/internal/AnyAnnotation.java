/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.Any;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.AttributeMarker;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AnyAnnotation implements Any, AttributeMarker, AttributeMarker.Fetchable, AttributeMarker.Optionalable {
	private jakarta.persistence.FetchType fetch;
	private boolean optional;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AnyAnnotation(SourceModelBuildingContext modelContext) {
		this.fetch = jakarta.persistence.FetchType.EAGER;
		this.optional = true;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AnyAnnotation(Any annotation, SourceModelBuildingContext modelContext) {
		this.fetch = annotation.fetch();
		this.optional = annotation.optional();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AnyAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.fetch = extractJandexValue( annotation, HibernateAnnotations.ANY, "fetch", modelContext );
		this.optional = extractJandexValue( annotation, HibernateAnnotations.ANY, "optional", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Any.class;
	}

	@Override
	public jakarta.persistence.FetchType fetch() {
		return fetch;
	}

	public void fetch(jakarta.persistence.FetchType value) {
		this.fetch = value;
	}


	@Override
	public boolean optional() {
		return optional;
	}

	public void optional(boolean value) {
		this.optional = value;
	}


}
