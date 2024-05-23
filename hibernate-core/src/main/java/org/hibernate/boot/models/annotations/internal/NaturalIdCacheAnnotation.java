/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NaturalIdCacheAnnotation implements NaturalIdCache {
	private String region;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public NaturalIdCacheAnnotation(SourceModelBuildingContext modelContext) {
		this.region = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NaturalIdCacheAnnotation(NaturalIdCache annotation, SourceModelBuildingContext modelContext) {
		this.region = extractJdkValue( annotation, HibernateAnnotations.NATURAL_ID_CACHE, "region", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NaturalIdCacheAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.region = extractJandexValue( annotation, HibernateAnnotations.NATURAL_ID_CACHE, "region", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return NaturalIdCache.class;
	}

	@Override
	public String region() {
		return region;
	}

	public void region(String value) {
		this.region = value;
	}


}
