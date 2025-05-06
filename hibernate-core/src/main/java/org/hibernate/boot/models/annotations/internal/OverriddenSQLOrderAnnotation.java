/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.SQLOrder;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.AbstractOverrider;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_SQL_ORDER;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenSQLOrderAnnotation
		extends AbstractOverrider<SQLOrder>
		implements DialectOverride.SQLOrder, DialectOverrider<SQLOrder> {
	private SQLOrder override;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenSQLOrderAnnotation(ModelsContext sourceModelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenSQLOrderAnnotation(
			DialectOverride.SQLOrder annotation,
			ModelsContext sourceModelContext) {
		dialect( annotation.dialect() );
		before( annotation.before() );
		sameOrAfter( annotation.sameOrAfter() );
		override( extractJdkValue( annotation, DIALECT_OVERRIDE_SQL_ORDER, "override", sourceModelContext ) );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenSQLOrderAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext sourceModelContext) {
		super( attributeValues, DIALECT_OVERRIDE_SQL_ORDER, sourceModelContext );
		override( (SQLOrder) attributeValues.get( "override" ) );
	}

	@Override
	public AnnotationDescriptor<SQLOrder> getOverriddenDescriptor() {
		return HibernateAnnotations.SQL_ORDER;
	}

	@Override
	public SQLOrder override() {
		return override;
	}

	public void override(SQLOrder value) {
		this.override = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.SQLOrder.class;
	}
}
