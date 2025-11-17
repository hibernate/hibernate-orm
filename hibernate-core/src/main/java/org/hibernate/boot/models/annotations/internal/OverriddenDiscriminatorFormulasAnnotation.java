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

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_DISCRIMINATOR_FORMULAS;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenDiscriminatorFormulasAnnotation
		implements DialectOverride.DiscriminatorFormulas,
		RepeatableContainer<DialectOverride.DiscriminatorFormula> {
	private DialectOverride.DiscriminatorFormula[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenDiscriminatorFormulasAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenDiscriminatorFormulasAnnotation(
			DialectOverride.DiscriminatorFormulas annotation,
			ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, DIALECT_OVERRIDE_DISCRIMINATOR_FORMULAS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenDiscriminatorFormulasAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (DialectOverride.DiscriminatorFormula[]) attributeValues.get( "value" );
	}

	@Override
	public DialectOverride.DiscriminatorFormula[] value() {
		return value;
	}

	@Override
	public void value(DialectOverride.DiscriminatorFormula[] value) {
		this.value = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.DiscriminatorFormulas.class;
	}
}
