/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

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

	public OverriddenSQLInsertsAnnotation(SourceModelBuildingContext sourceModelBuildingContext) {
	}

	public OverriddenSQLInsertsAnnotation(
			DialectOverride.SQLInserts annotation,
			SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue( annotation, DIALECT_OVERRIDE_SQL_INSERTS, "value", modelContext );
	}

	public OverriddenSQLInsertsAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJandexValue( annotation, DIALECT_OVERRIDE_SQL_INSERTS, "value", modelContext );
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
