/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.DialectOverride.SQLOrders;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.SourceModelBuildingContext;

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
	public OverriddenSQLOrdersAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenSQLOrdersAnnotation(SQLOrders annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue( annotation, DIALECT_OVERRIDE_SQL_ORDERS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenSQLOrdersAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
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
