/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.Check;
import org.hibernate.annotations.DialectOverride;
import org.hibernate.boot.models.DialectOverrideAnnotations;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.AbstractOverrider;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_CHECK;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenCheckAnnotation
		extends AbstractOverrider<Check>
		implements DialectOverride.Check, DialectOverrider<Check> {
	private Check override;

	public OverriddenCheckAnnotation(SourceModelBuildingContext modelContext) {
	}

	public OverriddenCheckAnnotation(DialectOverride.Check annotation, SourceModelBuildingContext modelContext) {
		dialect( annotation.dialect() );
		before( annotation.before() );
		sameOrAfter( annotation.sameOrAfter() );
		override( extractJdkValue( annotation, DIALECT_OVERRIDE_CHECK, "override", modelContext ) );
	}

	public OverriddenCheckAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		super( annotation, DIALECT_OVERRIDE_CHECK, modelContext );
		override( extractJandexValue( annotation, DIALECT_OVERRIDE_CHECK, "override", modelContext ) );
	}

	@Override
	public AnnotationDescriptor<Check> getOverriddenDescriptor() {
		return HibernateAnnotations.CHECK;
	}

	@Override
	public Check override() {
		return override;
	}

	public void override(Check value) {
		this.override = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.Check.class;
	}
}
