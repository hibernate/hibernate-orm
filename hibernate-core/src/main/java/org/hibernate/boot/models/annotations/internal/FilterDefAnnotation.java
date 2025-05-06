/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.FilterDef;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class FilterDefAnnotation implements FilterDef {
	private String name;
	private String defaultCondition;
	private boolean autoEnabled;
	private boolean applyToLoadByKey;
	private org.hibernate.annotations.ParamDef[] parameters;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public FilterDefAnnotation(ModelsContext modelContext) {
		this.defaultCondition = "";
		this.parameters = new org.hibernate.annotations.ParamDef[0];
		this.autoEnabled = false;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public FilterDefAnnotation(FilterDef annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.defaultCondition = annotation.defaultCondition();
		this.autoEnabled = annotation.autoEnabled();
		this.applyToLoadByKey = annotation.applyToLoadByKey();
		this.parameters = extractJdkValue( annotation, HibernateAnnotations.FILTER_DEF, "parameters", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public FilterDefAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.defaultCondition = (String) attributeValues.get( "defaultCondition" );
		this.autoEnabled = (boolean) attributeValues.get( "autoEnabled" );
		this.applyToLoadByKey = (boolean) attributeValues.get( "applyToLoadByKey" );
		this.parameters = (org.hibernate.annotations.ParamDef[]) attributeValues.get( "parameters" );
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
	public boolean autoEnabled() {
		return autoEnabled;
	}

	public void autoEnabled(boolean value) {
		this.autoEnabled = value;
	}


	@Override
	public boolean applyToLoadByKey() {
		return applyToLoadByKey;
	}

	public void applyToLoadByKey(boolean value) {
		this.applyToLoadByKey = value;
	}

	@Override
	public org.hibernate.annotations.ParamDef[] parameters() {
		return parameters;
	}

	public void parameters(org.hibernate.annotations.ParamDef[] value) {
		this.parameters = value;
	}


}
