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

import jakarta.persistence.ManyToMany;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ManyToManyJpaAnnotation implements ManyToMany,
		AttributeMarker.Fetchable,
		AttributeMarker.Cascadeable,
		AttributeMarker.Mappable {
	private java.lang.Class<?> targetEntity;
	private jakarta.persistence.CascadeType[] cascade;
	private jakarta.persistence.FetchType fetch;
	private String mappedBy;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ManyToManyJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.targetEntity = void.class;
		this.cascade = new jakarta.persistence.CascadeType[0];
		this.fetch = jakarta.persistence.FetchType.LAZY;
		this.mappedBy = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ManyToManyJpaAnnotation(ManyToMany annotation, SourceModelBuildingContext modelContext) {
		this.targetEntity = annotation.targetEntity();
		this.cascade = annotation.cascade();
		this.fetch = annotation.fetch();
		this.mappedBy = annotation.mappedBy();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ManyToManyJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.targetEntity = extractJandexValue( annotation, JpaAnnotations.MANY_TO_MANY, "targetEntity", modelContext );
		this.cascade = extractJandexValue( annotation, JpaAnnotations.MANY_TO_MANY, "cascade", modelContext );
		this.fetch = extractJandexValue( annotation, JpaAnnotations.MANY_TO_MANY, "fetch", modelContext );
		this.mappedBy = extractJandexValue( annotation, JpaAnnotations.MANY_TO_MANY, "mappedBy", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ManyToMany.class;
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
	public String mappedBy() {
		return mappedBy;
	}

	public void mappedBy(String value) {
		this.mappedBy = value;
	}


}
