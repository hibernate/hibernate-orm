/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_SQL_INSERTS;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenSQLInsertsAnnotation
		implements DialectOverride.SQLInserts, RepeatableContainer<DialectOverride.SQLInsert> {
	private DialectOverride.SQLInsert[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenSQLInsertsAnnotation(SourceModelBuildingContext sourceModelBuildingContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenSQLInsertsAnnotation(
			DialectOverride.SQLInserts annotation,
			SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue( annotation, DIALECT_OVERRIDE_SQL_INSERTS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenSQLInsertsAnnotation(
			Map<String, Object> attributeValues,
			SourceModelBuildingContext modelContext) {
		this.value = (DialectOverride.SQLInsert[]) attributeValues.get( "value" );
	}

	@Override
	public DialectOverride.SQLInsert[] value() {
		return value;
	}

	@Override
	public void value(DialectOverride.SQLInsert[] value) {
		this.value = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.SQLInserts.class;
	}
}
