/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.DialectOverride.SQLRestrictions;
import org.hibernate.boot.models.DialectOverrideAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class OverriddenSQLRestrictionsAnnotation
		implements DialectOverride.SQLRestrictions, RepeatableContainer<DialectOverride.SQLRestriction> {
	private DialectOverride.SQLRestriction[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenSQLRestrictionsAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenSQLRestrictionsAnnotation(SQLRestrictions annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue(
				annotation,
				DialectOverrideAnnotations.DIALECT_OVERRIDE_SQL_RESTRICTIONS,
				"value",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenSQLRestrictionsAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJandexValue(
				annotation,
				DialectOverrideAnnotations.DIALECT_OVERRIDE_SQL_RESTRICTIONS,
				"value",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SQLRestrictions.class;
	}

	@Override
	public DialectOverride.SQLRestriction[] value() {
		return value;
	}

	public void value(DialectOverride.SQLRestriction[] value) {
		this.value = value;
	}


}
