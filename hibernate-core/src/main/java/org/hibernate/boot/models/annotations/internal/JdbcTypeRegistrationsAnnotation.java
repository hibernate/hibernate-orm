/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeRegistrations;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.SourceModelBuildingContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class JdbcTypeRegistrationsAnnotation
		implements JdbcTypeRegistrations, RepeatableContainer<org.hibernate.annotations.JdbcTypeRegistration> {
	private org.hibernate.annotations.JdbcTypeRegistration[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public JdbcTypeRegistrationsAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public JdbcTypeRegistrationsAnnotation(JdbcTypeRegistrations annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue( annotation, HibernateAnnotations.JDBC_TYPE_REGISTRATIONS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public JdbcTypeRegistrationsAnnotation(
			Map<String, Object> attributeValues,
			SourceModelBuildingContext modelContext) {
		this.value = (org.hibernate.annotations.JdbcTypeRegistration[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return JdbcTypeRegistrations.class;
	}

	@Override
	public org.hibernate.annotations.JdbcTypeRegistration[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.JdbcTypeRegistration[] value) {
		this.value = value;
	}


}
