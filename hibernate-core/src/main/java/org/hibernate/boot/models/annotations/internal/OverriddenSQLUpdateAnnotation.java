/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.AbstractOverrider;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.SourceModelBuildingContext;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_SQL_UPDATE;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenSQLUpdateAnnotation
		extends AbstractOverrider<SQLUpdate>
		implements DialectOverride.SQLUpdate, DialectOverrider<SQLUpdate> {
	private SQLUpdate override;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenSQLUpdateAnnotation(SourceModelBuildingContext sourceModelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenSQLUpdateAnnotation(
			DialectOverride.SQLUpdate annotation,
			SourceModelBuildingContext sourceModelContext) {
		dialect( annotation.dialect() );
		before( annotation.before() );
		sameOrAfter( annotation.sameOrAfter() );
		override( extractJdkValue( annotation, DIALECT_OVERRIDE_SQL_UPDATE, "override", sourceModelContext ) );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenSQLUpdateAnnotation(
			Map<String, Object> attributeValues,
			SourceModelBuildingContext sourceModelContext) {
		super( attributeValues, DIALECT_OVERRIDE_SQL_UPDATE, sourceModelContext );
		override( (SQLUpdate) attributeValues.get( "override" ) );
	}

	@Override
	public AnnotationDescriptor<SQLUpdate> getOverriddenDescriptor() {
		return HibernateAnnotations.SQL_UPDATE;
	}

	@Override
	public SQLUpdate override() {
		return override;
	}

	public void override(SQLUpdate value) {
		this.override = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.SQLUpdate.class;
	}
}
