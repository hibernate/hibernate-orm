/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.FilterDef;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class FilterDefAnnotation implements FilterDef {
	private String name;
	private String defaultCondition;
	private org.hibernate.annotations.ParamDef[] parameters;
	private boolean autoEnabled;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public FilterDefAnnotation(SourceModelBuildingContext modelContext) {
		this.defaultCondition = "";
		this.parameters = new org.hibernate.annotations.ParamDef[0];
		this.autoEnabled = false;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public FilterDefAnnotation(FilterDef annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJdkValue( annotation, HibernateAnnotations.FILTER_DEF, "name", modelContext );
		this.defaultCondition = extractJdkValue(
				annotation,
				HibernateAnnotations.FILTER_DEF,
				"defaultCondition",
				modelContext
		);
		this.parameters = extractJdkValue( annotation, HibernateAnnotations.FILTER_DEF, "parameters", modelContext );
		this.autoEnabled = extractJdkValue( annotation, HibernateAnnotations.FILTER_DEF, "autoEnabled", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public FilterDefAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, HibernateAnnotations.FILTER_DEF, "name", modelContext );
		this.defaultCondition = extractJandexValue(
				annotation,
				HibernateAnnotations.FILTER_DEF,
				"defaultCondition",
				modelContext
		);
		this.parameters = extractJandexValue( annotation, HibernateAnnotations.FILTER_DEF, "parameters", modelContext );
		this.autoEnabled = extractJandexValue(
				annotation,
				HibernateAnnotations.FILTER_DEF,
				"autoEnabled",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return FilterDef.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public String defaultCondition() {
		return defaultCondition;
	}

	public void defaultCondition(String value) {
		this.defaultCondition = value;
	}


	@Override
	public org.hibernate.annotations.ParamDef[] parameters() {
		return parameters;
	}

	public void parameters(org.hibernate.annotations.ParamDef[] value) {
		this.parameters = value;
	}


	@Override
	public boolean autoEnabled() {
		return autoEnabled;
	}

	public void autoEnabled(boolean value) {
		this.autoEnabled = value;
	}


}
