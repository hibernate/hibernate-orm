/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.AbstractOverrider;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_JOIN_FORMULA;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenJoinFormulaAnnotation
		extends AbstractOverrider<JoinFormula>
		implements DialectOverride.JoinFormula, DialectOverrider<JoinFormula> {
	private JoinFormula override;

	public OverriddenJoinFormulaAnnotation(SourceModelBuildingContext sourceModelContext) {
	}

	public OverriddenJoinFormulaAnnotation(
			DialectOverride.JoinFormula source,
			SourceModelBuildingContext sourceModelContext) {
		dialect( source.dialect() );
		before( source.before() );
		sameOrAfter( source.sameOrAfter() );
		override( extractJdkValue( source, DIALECT_OVERRIDE_JOIN_FORMULA, "override", sourceModelContext ) );
	}

	public OverriddenJoinFormulaAnnotation(AnnotationInstance source, SourceModelBuildingContext sourceModelContext) {
		super( source, DIALECT_OVERRIDE_JOIN_FORMULA, sourceModelContext );
		override( extractJandexValue( source, DIALECT_OVERRIDE_JOIN_FORMULA, "override", sourceModelContext ) );
	}

	@Override
	public AnnotationDescriptor<JoinFormula> getOverriddenDescriptor() {
		return HibernateAnnotations.JOIN_FORMULA;
	}

	@Override
	public JoinFormula override() {
		return override;
	}

	public void override(JoinFormula value) {
		this.override = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.JoinFormula.class;
	}
}
