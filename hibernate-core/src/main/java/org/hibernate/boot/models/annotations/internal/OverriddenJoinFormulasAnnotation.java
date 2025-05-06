/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_JOIN_FORMULAS;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenJoinFormulasAnnotation
		implements DialectOverride.JoinFormulas, RepeatableContainer<DialectOverride.JoinFormula> {
	private DialectOverride.JoinFormula[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenJoinFormulasAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenJoinFormulasAnnotation(
			DialectOverride.JoinFormulas annotation,
			ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, DIALECT_OVERRIDE_JOIN_FORMULAS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenJoinFormulasAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (DialectOverride.JoinFormula[]) attributeValues.get( "value" );
	}

	@Override
	public DialectOverride.JoinFormula[] value() {
		return value;
	}

	@Override
	public void value(DialectOverride.JoinFormula[] value) {
		this.value = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.JoinFormulas.class;
	}
}
