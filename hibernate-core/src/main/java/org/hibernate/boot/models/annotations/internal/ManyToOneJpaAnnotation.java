/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.spi.AttributeMarker;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import jakarta.persistence.ManyToOne;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ManyToOneJpaAnnotation implements ManyToOne,
		AttributeMarker,
		AttributeMarker.Fetchable,
		AttributeMarker.Cascadeable,
		AttributeMarker.Optionalable {
	private java.lang.Class<?> targetEntity;
	private jakarta.persistence.CascadeType[] cascade;
	private jakarta.persistence.FetchType fetch;
	private boolean optional;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ManyToOneJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.targetEntity = void.class;
		this.cascade = new jakarta.persistence.CascadeType[0];
		this.fetch = jakarta.persistence.FetchType.EAGER;
		this.optional = true;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ManyToOneJpaAnnotation(ManyToOne annotation, SourceModelBuildingContext modelContext) {
		this.targetEntity = extractJdkValue( annotation, JpaAnnotations.MANY_TO_ONE, "targetEntity", modelContext );
		this.cascade = extractJdkValue( annotation, JpaAnnotations.MANY_TO_ONE, "cascade", modelContext );
		this.fetch = extractJdkValue( annotation, JpaAnnotations.MANY_TO_ONE, "fetch", modelContext );
		this.optional = extractJdkValue( annotation, JpaAnnotations.MANY_TO_ONE, "optional", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ManyToOneJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.targetEntity = extractJandexValue( annotation, JpaAnnotations.MANY_TO_ONE, "targetEntity", modelContext );
		this.cascade = extractJandexValue( annotation, JpaAnnotations.MANY_TO_ONE, "cascade", modelContext );
		this.fetch = extractJandexValue( annotation, JpaAnnotations.MANY_TO_ONE, "fetch", modelContext );
		this.optional = extractJandexValue( annotation, JpaAnnotations.MANY_TO_ONE, "optional", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ManyToOne.class;
	}

	@Override
	public java.lang.Class<?> targetEntity() {
		return targetEntity;
	}

	public void targetEntity(java.lang.Class<?> value) {
		this.targetEntity = value;
	}


	@Override
	public jakarta.persistence.CascadeType[] cascade() {
		return cascade;
	}

	public void cascade(jakarta.persistence.CascadeType[] value) {
		this.cascade = value;
	}


	@Override
	public jakarta.persistence.FetchType fetch() {
		return fetch;
	}

	public void fetch(jakarta.persistence.FetchType value) {
		this.fetch = value;
	}


	@Override
	public boolean optional() {
		return optional;
	}

	public void optional(boolean value) {
		this.optional = value;
	}


}
