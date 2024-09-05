/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.ManyToAny;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ManyToAnyAnnotation implements ManyToAny {
	private jakarta.persistence.FetchType fetch;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ManyToAnyAnnotation(SourceModelBuildingContext modelContext) {
		this.fetch = jakarta.persistence.FetchType.EAGER;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ManyToAnyAnnotation(ManyToAny annotation, SourceModelBuildingContext modelContext) {
		this.fetch = annotation.fetch();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ManyToAnyAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.fetch = extractJandexValue( annotation, HibernateAnnotations.MANY_TO_ANY, "fetch", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ManyToAny.class;
	}

	@Override
	public jakarta.persistence.FetchType fetch() {
		return fetch;
	}

	public void fetch(jakarta.persistence.FetchType value) {
		this.fetch = value;
	}


}
