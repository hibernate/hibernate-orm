/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class JoinColumnsOrFormulasAnnotation
		implements JoinColumnsOrFormulas, RepeatableContainer<JoinColumnOrFormula> {
	private org.hibernate.annotations.JoinColumnOrFormula[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public JoinColumnsOrFormulasAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public JoinColumnsOrFormulasAnnotation(JoinColumnsOrFormulas annotation, ModelsContext modelContext) {
		this.value = extractJdkValue(
				annotation,
				HibernateAnnotations.JOIN_COLUMNS_OR_FORMULAS,
				"value",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public JoinColumnsOrFormulasAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (JoinColumnOrFormula[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return JoinColumnsOrFormulas.class;
	}

	@Override
	public org.hibernate.annotations.JoinColumnOrFormula[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.JoinColumnOrFormula[] value) {
		this.value = value;
	}


}
