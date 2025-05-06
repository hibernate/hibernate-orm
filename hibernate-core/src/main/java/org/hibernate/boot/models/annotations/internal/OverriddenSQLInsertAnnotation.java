/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.AbstractOverrider;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_SQL_INSERT;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenSQLInsertAnnotation
		extends AbstractOverrider<SQLInsert>
		implements DialectOverride.SQLInsert, DialectOverrider<SQLInsert> {
	private SQLInsert override;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenSQLInsertAnnotation(ModelsContext sourceModelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenSQLInsertAnnotation(
			DialectOverride.SQLInsert annotation,
			ModelsContext sourceModelContext) {
		dialect( annotation.dialect() );
		before( annotation.before() );
		sameOrAfter( annotation.sameOrAfter() );
		override( extractJdkValue( annotation, DIALECT_OVERRIDE_SQL_INSERT, "override", sourceModelContext ) );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenSQLInsertAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext sourceModelContext) {
		super( attributeValues, DIALECT_OVERRIDE_SQL_INSERT, sourceModelContext );
		override( (SQLInsert) attributeValues.get( "override" ) );
	}

	@Override
	public AnnotationDescriptor<SQLInsert> getOverriddenDescriptor() {
		return HibernateAnnotations.SQL_INSERT;
	}

	@Override
	public SQLInsert override() {
		return override;
	}

	public void override(SQLInsert value) {
		this.override = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.SQLInsert.class;
	}
}
