/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class JoinColumnsJpaAnnotation implements JoinColumns, RepeatableContainer<JoinColumn> {
	private jakarta.persistence.JoinColumn[] value;
	private jakarta.persistence.ForeignKey foreignKey;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public JoinColumnsJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.foreignKey = JpaAnnotations.FOREIGN_KEY.createUsage( modelContext );
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public JoinColumnsJpaAnnotation(JoinColumns annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue( annotation, JpaAnnotations.JOIN_COLUMNS, "value", modelContext );
		this.foreignKey = extractJdkValue( annotation, JpaAnnotations.JOIN_COLUMNS, "foreignKey", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public JoinColumnsJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJandexValue( annotation, JpaAnnotations.JOIN_COLUMNS, "value", modelContext );
		this.foreignKey = extractJandexValue( annotation, JpaAnnotations.JOIN_COLUMNS, "foreignKey", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return JoinColumns.class;
	}

	@Override
	public jakarta.persistence.JoinColumn[] value() {
		return value;
	}

	public void value(jakarta.persistence.JoinColumn[] value) {
		this.value = value;
	}


	@Override
	public jakarta.persistence.ForeignKey foreignKey() {
		return foreignKey;
	}

	public void foreignKey(jakarta.persistence.ForeignKey value) {
		this.foreignKey = value;
	}


}
