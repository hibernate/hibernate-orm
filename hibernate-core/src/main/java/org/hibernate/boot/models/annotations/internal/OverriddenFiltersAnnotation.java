/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.Filters;
import org.hibernate.boot.models.DialectOverrideAnnotations;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.AbstractOverrider;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_FILTERS;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenFiltersAnnotation
		extends AbstractOverrider<Filters>
		implements DialectOverride.Filters, DialectOverrider<Filters> {
	private Filters override;

	public OverriddenFiltersAnnotation(SourceModelBuildingContext sourceModelContext) {
	}

	public OverriddenFiltersAnnotation(DialectOverride.Filters source, SourceModelBuildingContext sourceModelContext) {
		dialect( source.dialect() );
		before( source.before() );
		sameOrAfter( source.sameOrAfter() );
		override( extractJdkValue( source, DIALECT_OVERRIDE_FILTERS, "override", sourceModelContext ) );
	}

	public OverriddenFiltersAnnotation(AnnotationInstance source, SourceModelBuildingContext sourceModelContext) {
		super( source, DIALECT_OVERRIDE_FILTERS, sourceModelContext );
		override( extractJandexValue( source, DIALECT_OVERRIDE_FILTERS, "override", sourceModelContext ) );
	}

	@Override
	public AnnotationDescriptor<Filters> getOverriddenDescriptor() {
		return HibernateAnnotations.FILTERS;
	}

	@Override
	public Filters override() {
		return override;
	}

	public void override(Filters value) {
		this.override = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.Filters.class;
	}
}
