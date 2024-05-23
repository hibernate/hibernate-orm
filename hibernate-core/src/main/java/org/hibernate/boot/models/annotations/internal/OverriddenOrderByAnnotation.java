/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.OrderBy;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.AbstractOverrider;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_ORDER_BY;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenOrderByAnnotation
		extends AbstractOverrider<OrderBy>
		implements DialectOverride.OrderBy, DialectOverrider<OrderBy> {
	private OrderBy override;

	public OverriddenOrderByAnnotation(SourceModelBuildingContext sourceModelContext) {
	}

	public OverriddenOrderByAnnotation(DialectOverride.OrderBy source, SourceModelBuildingContext sourceModelContext) {
		dialect( source.dialect() );
		before( source.before() );
		sameOrAfter( source.sameOrAfter() );
		override( extractJdkValue( source, DIALECT_OVERRIDE_ORDER_BY, "override", sourceModelContext ) );
	}

	public OverriddenOrderByAnnotation(AnnotationInstance source, SourceModelBuildingContext sourceModelContext) {
		super( source, DIALECT_OVERRIDE_ORDER_BY, sourceModelContext );
		override( extractJandexValue( source, DIALECT_OVERRIDE_ORDER_BY, "override", sourceModelContext ) );
	}

	@Override
	public AnnotationDescriptor<OrderBy> getOverriddenDescriptor() {
		return HibernateAnnotations.ORDER_BY;
	}

	@Override
	public OrderBy override() {
		return override;
	}

	public void override(OrderBy value) {
		this.override = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.OrderBy.class;
	}
}
