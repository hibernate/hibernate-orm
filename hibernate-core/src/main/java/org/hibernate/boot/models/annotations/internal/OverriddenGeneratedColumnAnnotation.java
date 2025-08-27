/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.GeneratedColumn;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.AbstractOverrider;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_GENERATED_COLUMN;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenGeneratedColumnAnnotation
		extends AbstractOverrider<GeneratedColumn>
		implements DialectOverride.GeneratedColumn, DialectOverrider<GeneratedColumn> {
	private GeneratedColumn override;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenGeneratedColumnAnnotation(ModelsContext sourceModelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenGeneratedColumnAnnotation(
			DialectOverride.GeneratedColumn annotation,
			ModelsContext sourceModelContext) {
		dialect( annotation.dialect() );
		before( annotation.before() );
		sameOrAfter( annotation.sameOrAfter() );
		override( extractJdkValue( annotation, DIALECT_OVERRIDE_GENERATED_COLUMN, "override", sourceModelContext ) );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenGeneratedColumnAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext sourceModelContext) {
		super( attributeValues, DIALECT_OVERRIDE_GENERATED_COLUMN, sourceModelContext );
		override( (GeneratedColumn) attributeValues.get( "override" ) );
	}

	@Override
	public AnnotationDescriptor<GeneratedColumn> getOverriddenDescriptor() {
		return HibernateAnnotations.GENERATED_COLUMN;
	}

	@Override
	public GeneratedColumn override() {
		return override;
	}

	public void override(GeneratedColumn value) {
		this.override = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.GeneratedColumn.class;
	}
}
