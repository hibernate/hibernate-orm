/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.JoinFormula;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class JoinFormulaAnnotation implements JoinFormula {
	private String value;
	private String referencedColumnName;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public JoinFormulaAnnotation(ModelsContext modelContext) {
		this.referencedColumnName = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public JoinFormulaAnnotation(JoinFormula annotation, ModelsContext modelContext) {
		this.value = annotation.value();
		this.referencedColumnName = annotation.referencedColumnName();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public JoinFormulaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (String) attributeValues.get( "value" );
		this.referencedColumnName = (String) attributeValues.get( "referencedColumnName" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return JoinFormula.class;
	}

	@Override
	public String value() {
		return value;
	}

	public void value(String value) {
		this.value = value;
	}


	@Override
	public String referencedColumnName() {
		return referencedColumnName;
	}

	public void referencedColumnName(String value) {
		this.referencedColumnName = value;
	}


}
