/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.SQLOrder;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.AbstractOverrider;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_SQL_ORDER;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenSQLOrderAnnotation
		extends AbstractOverrider<SQLOrder>
		implements DialectOverride.SQLOrder, DialectOverrider<SQLOrder> {
	private SQLOrder override;

	public OverriddenSQLOrderAnnotation(SourceModelBuildingContext sourceModelContext) {
	}

	public OverriddenSQLOrderAnnotation(
			DialectOverride.SQLOrder source,
			SourceModelBuildingContext sourceModelContext) {
		dialect( source.dialect() );
		before( source.before() );
		sameOrAfter( source.sameOrAfter() );
		override( extractJdkValue( source, DIALECT_OVERRIDE_SQL_ORDER, "override", sourceModelContext ) );
	}

	public OverriddenSQLOrderAnnotation(AnnotationInstance source, SourceModelBuildingContext sourceModelContext) {
		super( source, DIALECT_OVERRIDE_SQL_ORDER, sourceModelContext );
		override( extractJandexValue( source, DIALECT_OVERRIDE_SQL_ORDER, "override", sourceModelContext ) );
	}

	@Override
	public AnnotationDescriptor<SQLOrder> getOverriddenDescriptor() {
		return HibernateAnnotations.SQL_ORDER;
	}

	@Override
	public SQLOrder override() {
		return override;
	}

	public void override(SQLOrder value) {
		this.override = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.SQLOrder.class;
	}
}
