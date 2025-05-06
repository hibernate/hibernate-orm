/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.Filters;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.AbstractOverrider;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_FILTERS;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenFiltersAnnotation
		extends AbstractOverrider<Filters>
		implements DialectOverride.Filters, DialectOverrider<Filters> {
	private Filters override;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenFiltersAnnotation(ModelsContext sourceModelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenFiltersAnnotation(
			DialectOverride.Filters annotation,
			ModelsContext sourceModelContext) {
		dialect( annotation.dialect() );
		before( annotation.before() );
		sameOrAfter( annotation.sameOrAfter() );
		override( extractJdkValue( annotation, DIALECT_OVERRIDE_FILTERS, "override", sourceModelContext ) );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenFiltersAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext sourceModelContext) {
		super( attributeValues, DIALECT_OVERRIDE_FILTERS, sourceModelContext );
		override( (Filters) attributeValues.get( "override" ) );
	}

	@Override
	public AnnotationDescriptor<Filters> getOverriddenDescriptor() {
		return HibernateAnnotations.FILTERS;
	}

	@Override
	public Filters override() {
		return override;
	}

	public void override(Filters value) {
		this.override = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.Filters.class;
	}
}
