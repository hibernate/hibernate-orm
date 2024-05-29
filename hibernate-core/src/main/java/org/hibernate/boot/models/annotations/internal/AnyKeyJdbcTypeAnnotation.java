/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.AnyKeyJdbcType;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AnyKeyJdbcTypeAnnotation implements AnyKeyJdbcType {
	private Class<? extends JdbcType> value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AnyKeyJdbcTypeAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AnyKeyJdbcTypeAnnotation(AnyKeyJdbcType annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AnyKeyJdbcTypeAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJandexValue(
				annotation,
				org.hibernate.boot.models.HibernateAnnotations.ANY_KEY_JDBC_TYPE,
				"value",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return AnyKeyJdbcType.class;
	}

	@Override
	public Class<? extends JdbcType> value() {
		return value;
	}

	public void value(Class<? extends JdbcType> value) {
		this.value = value;
	}


}
