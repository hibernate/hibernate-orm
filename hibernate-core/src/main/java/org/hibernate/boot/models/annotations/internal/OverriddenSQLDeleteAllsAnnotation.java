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
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.SourceModelBuildingContext;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_SQL_DELETE_ALLS;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenSQLDeleteAllsAnnotation
		implements DialectOverride.SQLDeleteAlls, RepeatableContainer<DialectOverride.SQLDeleteAll> {
	private DialectOverride.SQLDeleteAll[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenSQLDeleteAllsAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenSQLDeleteAllsAnnotation(
			DialectOverride.SQLDeleteAlls annotation,
			SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue( annotation, DIALECT_OVERRIDE_SQL_DELETE_ALLS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenSQLDeleteAllsAnnotation(
			Map<String, Object> attributeValues,
			SourceModelBuildingContext modelContext) {
		this.value = (DialectOverride.SQLDeleteAll[]) attributeValues.get( "value" );
	}

	@Override
	public DialectOverride.SQLDeleteAll[] value() {
		return value;
	}

	@Override
	public void value(DialectOverride.SQLDeleteAll[] value) {
		this.value = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.SQLDeleteAlls.class;
	}
}
