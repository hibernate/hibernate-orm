/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.AbstractOverrider;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_SQL_UPDATE;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenSQLUpdateAnnotation
		extends AbstractOverrider<SQLUpdate>
		implements DialectOverride.SQLUpdate, DialectOverrider<SQLUpdate> {
	private SQLUpdate override;

	public OverriddenSQLUpdateAnnotation(SourceModelBuildingContext sourceModelContext) {
	}

	public OverriddenSQLUpdateAnnotation(
			DialectOverride.SQLUpdate source,
			SourceModelBuildingContext sourceModelContext) {
		dialect( source.dialect() );
		before( source.before() );
		sameOrAfter( source.sameOrAfter() );
		override( extractJdkValue( source, DIALECT_OVERRIDE_SQL_UPDATE, "override", sourceModelContext ) );
	}

	public OverriddenSQLUpdateAnnotation(AnnotationInstance source, SourceModelBuildingContext sourceModelContext) {
		super( source, DIALECT_OVERRIDE_SQL_UPDATE, sourceModelContext );
		override( extractJandexValue( source, DIALECT_OVERRIDE_SQL_UPDATE, "override", sourceModelContext ) );
	}

	@Override
	public AnnotationDescriptor<SQLUpdate> getOverriddenDescriptor() {
		return HibernateAnnotations.SQL_UPDATE;
	}

	@Override
	public SQLUpdate override() {
		return override;
	}

	public void override(SQLUpdate value) {
		this.override = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.SQLUpdate.class;
	}
}
