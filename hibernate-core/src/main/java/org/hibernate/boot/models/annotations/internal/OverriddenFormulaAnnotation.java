/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.Formula;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.AbstractOverrider;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.SourceModelBuildingContext;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_FORMULA;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenFormulaAnnotation
		extends AbstractOverrider<Formula>
		implements DialectOverride.Formula, DialectOverrider<Formula> {
	private Formula override;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenFormulaAnnotation(SourceModelBuildingContext sourceModelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenFormulaAnnotation(
			DialectOverride.Formula annotation,
			SourceModelBuildingContext sourceModelContext) {
		dialect( annotation.dialect() );
		before( annotation.before() );
		sameOrAfter( annotation.sameOrAfter() );
		override( extractJdkValue( annotation, DIALECT_OVERRIDE_FORMULA, "override", sourceModelContext ) );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenFormulaAnnotation(
			Map<String, Object> attributeValues,
			SourceModelBuildingContext sourceModelContext) {
		super( attributeValues, DIALECT_OVERRIDE_FORMULA, sourceModelContext );
		override( (Formula) attributeValues.get( "override" ) );
	}

	@Override
	public AnnotationDescriptor<Formula> getOverriddenDescriptor() {
		return HibernateAnnotations.FORMULA;
	}

	@Override
	public Formula override() {
		return override;
	}

	public void override(Formula value) {
		this.override = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.Formula.class;
	}
}
