/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.CollectionIdJdbcType;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CollectionIdJdbcTypeAnnotation implements CollectionIdJdbcType {
	private Class<? extends JdbcType> value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CollectionIdJdbcTypeAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CollectionIdJdbcTypeAnnotation(CollectionIdJdbcType annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue( annotation, HibernateAnnotations.COLLECTION_ID_JDBC_TYPE, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CollectionIdJdbcTypeAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJandexValue(
				annotation,
				HibernateAnnotations.COLLECTION_ID_JDBC_TYPE,
				"value",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return CollectionIdJdbcType.class;
	}

	@Override
	public Class<? extends JdbcType> value() {
		return value;
	}

	public void value(Class<? extends JdbcType> value) {
		this.value = value;
	}


}
