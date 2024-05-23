/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.AbstractOverrider;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_FILTER_DEFS;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenFilterDefsAnnotation
		extends AbstractOverrider<FilterDefs>
		implements DialectOverride.FilterDefs, DialectOverrider<FilterDefs> {
	private FilterDefs override;

	public OverriddenFilterDefsAnnotation(SourceModelBuildingContext modelContext) {
	}

	public OverriddenFilterDefsAnnotation(DialectOverride.FilterDefs source, SourceModelBuildingContext modelContext) {
		dialect( source.dialect() );
		before( source.before() );
		sameOrAfter( source.sameOrAfter() );
		override( extractJdkValue( source, DIALECT_OVERRIDE_FILTER_DEFS, "override", modelContext ) );
	}

	public OverriddenFilterDefsAnnotation(AnnotationInstance source, SourceModelBuildingContext modelContext) {
		super( source, DIALECT_OVERRIDE_FILTER_DEFS, modelContext );
		override( extractJandexValue( source, DIALECT_OVERRIDE_FILTER_DEFS, "override", modelContext ) );
	}

	@Override
	public AnnotationDescriptor<FilterDefs> getOverriddenDescriptor() {
		return HibernateAnnotations.FILTER_DEFS;
	}

	@Override
	public FilterDefs override() {
		return override;
	}

	public void override(FilterDefs value) {
		this.override = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.FilterDefs.class;
	}
}
