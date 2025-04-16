/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.AbstractOverrider;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_SQL_DELETE;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenSQLDeleteAnnotation
		extends AbstractOverrider<SQLDelete>
		implements DialectOverride.SQLDelete, DialectOverrider<SQLDelete> {
	private SQLDelete override;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenSQLDeleteAnnotation(ModelsContext sourceModelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenSQLDeleteAnnotation(
			DialectOverride.SQLDelete annotation,
			ModelsContext sourceModelContext) {
		dialect( annotation.dialect() );
		before( annotation.before() );
		sameOrAfter( annotation.sameOrAfter() );
		override( extractJdkValue( annotation, DIALECT_OVERRIDE_SQL_DELETE, "override", sourceModelContext ) );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenSQLDeleteAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext sourceModelContext) {
		super( attributeValues, DIALECT_OVERRIDE_SQL_DELETE, sourceModelContext );
		override( (SQLDelete) attributeValues.get( "override" ) );
	}

	@Override
	public AnnotationDescriptor<SQLDelete> getOverriddenDescriptor() {
		return HibernateAnnotations.SQL_DELETE;
	}

	@Override
	public SQLDelete override() {
		return override;
	}

	public void override(SQLDelete value) {
		this.override = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.SQLDelete.class;
	}
}
