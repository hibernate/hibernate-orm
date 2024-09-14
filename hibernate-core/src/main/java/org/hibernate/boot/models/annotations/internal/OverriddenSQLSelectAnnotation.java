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
import org.hibernate.annotations.SQLSelect;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.AbstractOverrider;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.SourceModelBuildingContext;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_SQL_SELECT;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenSQLSelectAnnotation
		extends AbstractOverrider<SQLSelect>
		implements DialectOverride.SQLSelect, DialectOverrider<SQLSelect> {
	private SQLSelect override;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenSQLSelectAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenSQLSelectAnnotation(
			DialectOverride.SQLSelect annotation,
			SourceModelBuildingContext modelContext) {
		dialect( annotation.dialect() );
		before( annotation.before() );
		sameOrAfter( annotation.sameOrAfter() );
		override( extractJdkValue( annotation, DIALECT_OVERRIDE_SQL_SELECT, "override", modelContext ) );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenSQLSelectAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		super( attributeValues, DIALECT_OVERRIDE_SQL_SELECT, modelContext );
		override( (SQLSelect) attributeValues.get( "override" ) );
	}

	@Override
	public AnnotationDescriptor<SQLSelect> getOverriddenDescriptor() {
		return HibernateAnnotations.SQL_SELECT;
	}

	@Override
	public SQLSelect override() {
		return override;
	}

	public void override(SQLSelect value) {
		this.override = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.SQLSelect.class;
	}
}
