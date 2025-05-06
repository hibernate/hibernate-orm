/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.DialectOverride.SQLOrders;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_SQL_ORDERS;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class OverriddenSQLOrdersAnnotation
		implements DialectOverride.SQLOrders, RepeatableContainer<DialectOverride.SQLOrder> {
	private DialectOverride.SQLOrder[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenSQLOrdersAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenSQLOrdersAnnotation(SQLOrders annotation, ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, DIALECT_OVERRIDE_SQL_ORDERS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenSQLOrdersAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (DialectOverride.SQLOrder[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SQLOrders.class;
	}

	@Override
	public DialectOverride.SQLOrder[] value() {
		return value;
	}

	public void value(DialectOverride.SQLOrder[] value) {
		this.value = value;
	}


}
