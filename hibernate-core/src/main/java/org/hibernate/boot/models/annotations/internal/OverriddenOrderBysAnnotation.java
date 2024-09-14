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

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_ORDER_BYS;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenOrderBysAnnotation
		implements DialectOverride.OrderBys, RepeatableContainer<DialectOverride.OrderBy> {
	private DialectOverride.OrderBy[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenOrderBysAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenOrderBysAnnotation(DialectOverride.OrderBys annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue( annotation, DIALECT_OVERRIDE_ORDER_BYS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenOrderBysAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.value = (DialectOverride.OrderBy[]) attributeValues.get( "value" );
	}

	@Override
	public DialectOverride.OrderBy[] value() {
		return value;
	}

	@Override
	public void value(DialectOverride.OrderBy[] value) {
		this.value = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.OrderBys.class;
	}
}
