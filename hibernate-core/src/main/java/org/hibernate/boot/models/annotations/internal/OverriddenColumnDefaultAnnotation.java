/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DialectOverride;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.AbstractOverrider;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.SourceModelBuildingContext;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_COLUMN_DEFAULT;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenColumnDefaultAnnotation
		extends AbstractOverrider<ColumnDefault>
		implements DialectOverride.ColumnDefault, DialectOverrider<ColumnDefault> {
	private ColumnDefault override;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenColumnDefaultAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenColumnDefaultAnnotation(
			DialectOverride.ColumnDefault annotation,
			SourceModelBuildingContext modelContext) {
		dialect( annotation.dialect() );
		before( annotation.before() );
		sameOrAfter( annotation.sameOrAfter() );
		override( extractJdkValue( annotation, DIALECT_OVERRIDE_COLUMN_DEFAULT, "override", modelContext ) );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenColumnDefaultAnnotation(
			Map<String, Object> attributeValues,
			SourceModelBuildingContext modelContext) {
		super( attributeValues, DIALECT_OVERRIDE_COLUMN_DEFAULT, modelContext );
		override( (ColumnDefault) attributeValues.get( "override" ) );
	}

	@Override
	public AnnotationDescriptor<ColumnDefault> getOverriddenDescriptor() {
		return HibernateAnnotations.COLUMN_DEFAULT;
	}

	@Override
	public ColumnDefault override() {
		return override;
	}

	public void override(ColumnDefault value) {
		this.override = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.ColumnDefault.class;
	}
}
