/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.QueryCacheLayout;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class QueryCacheLayoutAnnotation implements QueryCacheLayout {
	private org.hibernate.annotations.CacheLayout layout;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public QueryCacheLayoutAnnotation(SourceModelBuildingContext modelContext) {
		this.layout = org.hibernate.annotations.CacheLayout.AUTO;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public QueryCacheLayoutAnnotation(QueryCacheLayout annotation, SourceModelBuildingContext modelContext) {
		this.layout = extractJdkValue( annotation, HibernateAnnotations.QUERY_CACHE_LAYOUT, "layout", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public QueryCacheLayoutAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.layout = extractJandexValue( annotation, HibernateAnnotations.QUERY_CACHE_LAYOUT, "layout", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return QueryCacheLayout.class;
	}

	@Override
	public org.hibernate.annotations.CacheLayout layout() {
		return layout;
	}

	public void layout(org.hibernate.annotations.CacheLayout value) {
		this.layout = value;
	}


}
