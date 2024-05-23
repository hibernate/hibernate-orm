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

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_WHERES;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenWheresAnnotation
		implements DialectOverride.Wheres, RepeatableContainer<DialectOverride.Where> {
	private DialectOverride.Where[] value;

	public OverriddenWheresAnnotation(SourceModelBuildingContext sourceModelContext) {
	}

	public OverriddenWheresAnnotation(DialectOverride.Wheres source, SourceModelBuildingContext sourceModelContext) {
		value = extractJdkValue( source, DIALECT_OVERRIDE_WHERES, "override", sourceModelContext );
	}

	public OverriddenWheresAnnotation(AnnotationInstance source, SourceModelBuildingContext sourceModelContext) {
		value = extractJandexValue( source, DIALECT_OVERRIDE_WHERES, "override", sourceModelContext );
	}

	@Override
	public DialectOverride.Where[] value() {
		return value;
	}

	@Override
	public void value(DialectOverride.Where[] value) {
		this.value = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.Wheres.class;
	}
}
