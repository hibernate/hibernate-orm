/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DialectOverride;
import org.hibernate.boot.models.DialectOverrideAnnotations;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.AbstractOverrider;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_COLUMN_DEFAULT;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenColumnDefaultAnnotation
		extends AbstractOverrider<ColumnDefault>
		implements DialectOverride.ColumnDefault, DialectOverrider<ColumnDefault> {
	private ColumnDefault override;

	public OverriddenColumnDefaultAnnotation(SourceModelBuildingContext modelContext) {
	}

	public OverriddenColumnDefaultAnnotation(
			DialectOverride.ColumnDefault annotation,
			SourceModelBuildingContext modelContext) {
		dialect( annotation.dialect() );
		before( annotation.before() );
		sameOrAfter( annotation.sameOrAfter() );
		override( extractJdkValue( annotation, DIALECT_OVERRIDE_COLUMN_DEFAULT, "override", modelContext ) );
	}

	public OverriddenColumnDefaultAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		super( annotation, DIALECT_OVERRIDE_COLUMN_DEFAULT, modelContext );
		override( extractJandexValue( annotation, DIALECT_OVERRIDE_COLUMN_DEFAULT, "override", modelContext ) );
	}

	@Override
	public AnnotationDescriptor<ColumnDefault> getOverriddenDescriptor() {
		return HibernateAnnotations.COLUMN_DEFAULT;
	}

	@Override
	public ColumnDefault override() {
		return override;
	}

	public void override(ColumnDefault value) {
		this.override = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.ColumnDefault.class;
	}
}
