/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedHqlQueryImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.xml.internal.QueryProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.NamedQuery;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;
import static org.hibernate.internal.util.NullnessHelper.coalesce;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NamedQueryJpaAnnotation implements NamedQuery {
	private String name;
	private String query;
	private java.lang.Class<?> resultClass;
	private jakarta.persistence.LockModeType lockMode;
	private jakarta.persistence.QueryHint[] hints;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public NamedQueryJpaAnnotation(ModelsContext modelContext) {
		this.resultClass = void.class;
		this.lockMode = jakarta.persistence.LockModeType.NONE;
		this.hints = new jakarta.persistence.QueryHint[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NamedQueryJpaAnnotation(NamedQuery annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.query = annotation.query();
		this.resultClass = annotation.resultClass();
		this.lockMode = annotation.lockMode();
		this.hints = extractJdkValue( annotation, JpaAnnotations.NAMED_QUERY, "hints", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NamedQueryJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.query = (String) attributeValues.get( "query" );
		this.resultClass = (Class<?>) attributeValues.get( "resultClass" );
		this.lockMode = (jakarta.persistence.LockModeType) attributeValues.get( "lockMode" );
		this.hints = (jakarta.persistence.QueryHint[]) attributeValues.get( "hints" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return NamedQuery.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public String query() {
		return query;
	}

	public void query(String value) {
		this.query = value;
	}


	@Override
	public java.lang.Class<?> resultClass() {
		return resultClass;
	}

	public void resultClass(java.lang.Class<?> value) {
		this.resultClass = value;
	}


	@Override
	public jakarta.persistence.LockModeType lockMode() {
		return lockMode;
	}

	public void lockMode(jakarta.persistence.LockModeType value) {
		this.lockMode = value;
	}


	@Override
	public jakarta.persistence.QueryHint[] hints() {
		return hints;
	}

	public void hints(jakarta.persistence.QueryHint[] value) {
		this.hints = value;
	}


	public void apply(JaxbNamedHqlQueryImpl jaxbNamedQuery, XmlDocumentContext xmlDocumentContext) {
		name( jaxbNamedQuery.getName() );
		query( jaxbNamedQuery.getQuery() );
		lockMode( coalesce( jaxbNamedQuery.getLockMode(), jakarta.persistence.LockModeType.NONE ) );

		hints( QueryProcessing.collectQueryHints( jaxbNamedQuery.getHints(), xmlDocumentContext ) );
	}
}
