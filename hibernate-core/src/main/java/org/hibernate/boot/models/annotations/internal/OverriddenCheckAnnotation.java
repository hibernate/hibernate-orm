/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Check;
import org.hibernate.annotations.DialectOverride;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.AbstractOverrider;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_CHECK;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenCheckAnnotation
		extends AbstractOverrider<Check>
		implements DialectOverride.Check, DialectOverrider<Check> {
	private Check override;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenCheckAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenCheckAnnotation(DialectOverride.Check annotation, ModelsContext modelContext) {
		dialect( annotation.dialect() );
		before( annotation.before() );
		sameOrAfter( annotation.sameOrAfter() );
		override( extractJdkValue( annotation, DIALECT_OVERRIDE_CHECK, "override", modelContext ) );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenCheckAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		super( attributeValues, DIALECT_OVERRIDE_CHECK, modelContext );
		override( (Check) attributeValues.get( "override" ) );
	}

	@Override
	public AnnotationDescriptor<Check> getOverriddenDescriptor() {
		return HibernateAnnotations.CHECK;
	}

	@Override
	public Check override() {
		return override;
	}

	public void override(Check value) {
		this.override = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.Check.class;
	}
}
