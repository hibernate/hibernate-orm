/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.AbstractOverrider;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_FILTER_DEFS;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenFilterDefsAnnotation
		extends AbstractOverrider<FilterDefs>
		implements DialectOverride.FilterDefs, DialectOverrider<FilterDefs> {
	private FilterDefs override;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenFilterDefsAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenFilterDefsAnnotation(
			DialectOverride.FilterDefs annotation,
			ModelsContext modelContext) {
		dialect( annotation.dialect() );
		before( annotation.before() );
		sameOrAfter( annotation.sameOrAfter() );
		override( extractJdkValue( annotation, DIALECT_OVERRIDE_FILTER_DEFS, "override", modelContext ) );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenFilterDefsAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		super( attributeValues, DIALECT_OVERRIDE_FILTER_DEFS, modelContext );
		override( (FilterDefs) attributeValues.get( "override" ) );
	}

	@Override
	public AnnotationDescriptor<FilterDefs> getOverriddenDescriptor() {
		return HibernateAnnotations.FILTER_DEFS;
	}

	@Override
	public FilterDefs override() {
		return override;
	}

	public void override(FilterDefs value) {
		this.override = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.FilterDefs.class;
	}
}
