/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.AbstractOverrider;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_DISCRIMINATOR_FORMULA;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenDiscriminatorFormulaAnnotation
		extends AbstractOverrider<DiscriminatorFormula>
		implements DialectOverride.DiscriminatorFormula, DialectOverrider<DiscriminatorFormula> {
	private DiscriminatorFormula override;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenDiscriminatorFormulaAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenDiscriminatorFormulaAnnotation(
			DialectOverride.DiscriminatorFormula annotation,
			ModelsContext modelContext) {
		dialect( annotation.dialect() );
		before( annotation.before() );
		sameOrAfter( annotation.sameOrAfter() );
		override( extractJdkValue( annotation, DIALECT_OVERRIDE_DISCRIMINATOR_FORMULA, "override", modelContext ) );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenDiscriminatorFormulaAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		super( attributeValues, DIALECT_OVERRIDE_DISCRIMINATOR_FORMULA, modelContext );
		override( (DiscriminatorFormula) attributeValues.get( "override" ) );
	}

	@Override
	public AnnotationDescriptor<DiscriminatorFormula> getOverriddenDescriptor() {
		return HibernateAnnotations.DISCRIMINATOR_FORMULA;
	}

	@Override
	public DiscriminatorFormula override() {
		return override;
	}

	public void override(DiscriminatorFormula value) {
		this.override = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.DiscriminatorFormula.class;
	}
}
