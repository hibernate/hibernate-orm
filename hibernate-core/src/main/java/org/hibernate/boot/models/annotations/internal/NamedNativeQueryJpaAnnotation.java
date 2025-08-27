/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedNativeQueryImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.xml.internal.QueryProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.NamedNativeQuery;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NamedNativeQueryJpaAnnotation implements NamedNativeQuery {
	private String name;
	private String query;
	private jakarta.persistence.QueryHint[] hints;
	private java.lang.Class<?> resultClass;
	private String resultSetMapping;
	private jakarta.persistence.EntityResult[] entities;
	private jakarta.persistence.ConstructorResult[] classes;
	private jakarta.persistence.ColumnResult[] columns;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public NamedNativeQueryJpaAnnotation(ModelsContext modelContext) {
		this.hints = new jakarta.persistence.QueryHint[0];
		this.resultClass = void.class;
		this.resultSetMapping = "";
		this.entities = new jakarta.persistence.EntityResult[0];
		this.classes = new jakarta.persistence.ConstructorResult[0];
		this.columns = new jakarta.persistence.ColumnResult[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NamedNativeQueryJpaAnnotation(NamedNativeQuery annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.query = annotation.query();
		this.hints = extractJdkValue( annotation, JpaAnnotations.NAMED_NATIVE_QUERY, "hints", modelContext );
		this.resultClass = annotation.resultClass();
		this.resultSetMapping = annotation.resultSetMapping();
		this.entities = extractJdkValue( annotation, JpaAnnotations.NAMED_NATIVE_QUERY, "entities", modelContext );
		this.classes = extractJdkValue( annotation, JpaAnnotations.NAMED_NATIVE_QUERY, "classes", modelContext );
		this.columns = extractJdkValue( annotation, JpaAnnotations.NAMED_NATIVE_QUERY, "columns", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NamedNativeQueryJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.query = (String) attributeValues.get( "query" );
		this.hints = (jakarta.persistence.QueryHint[]) attributeValues.get( "hints" );
		this.resultClass = (Class<?>) attributeValues.get( "resultClass" );
		this.resultSetMapping = (String) attributeValues.get( "resultSetMapping" );
		this.entities = (jakarta.persistence.EntityResult[]) attributeValues.get( "entities" );
		this.classes = (jakarta.persistence.ConstructorResult[]) attributeValues.get( "classes" );
		this.columns = (jakarta.persistence.ColumnResult[]) attributeValues.get( "columns" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return NamedNativeQuery.class;
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
	public jakarta.persistence.QueryHint[] hints() {
		return hints;
	}

	public void hints(jakarta.persistence.QueryHint[] value) {
		this.hints = value;
	}


	@Override
	public java.lang.Class<?> resultClass() {
		return resultClass;
	}

	public void resultClass(java.lang.Class<?> value) {
		this.resultClass = value;
	}


	@Override
	public String resultSetMapping() {
		return resultSetMapping;
	}

	public void resultSetMapping(String value) {
		this.resultSetMapping = value;
	}


	@Override
	public jakarta.persistence.EntityResult[] entities() {
		return entities;
	}

	public void entities(jakarta.persistence.EntityResult[] value) {
		this.entities = value;
	}


	@Override
	public jakarta.persistence.ConstructorResult[] classes() {
		return classes;
	}

	public void classes(jakarta.persistence.ConstructorResult[] value) {
		this.classes = value;
	}


	@Override
	public jakarta.persistence.ColumnResult[] columns() {
		return columns;
	}

	public void columns(jakarta.persistence.ColumnResult[] value) {
		this.columns = value;
	}


	public void apply(JaxbNamedNativeQueryImpl jaxbNamedQuery, XmlDocumentContext xmlDocumentContext) {
		name( jaxbNamedQuery.getName() );
		query( jaxbNamedQuery.getQuery() );

		hints( QueryProcessing.collectQueryHints( jaxbNamedQuery.getHints(), xmlDocumentContext ) );

		if ( StringHelper.isNotEmpty( jaxbNamedQuery.getResultClass() ) ) {
			final MutableClassDetails resultClassDetails = xmlDocumentContext.resolveJavaType( jaxbNamedQuery.getResultClass() );
			resultClass( resultClassDetails.toJavaClass() );
		}

		if ( StringHelper.isNotEmpty( jaxbNamedQuery.getResultSetMapping() ) ) {
			resultSetMapping( jaxbNamedQuery.getResultSetMapping() );
		}

		columns( QueryProcessing.extractColumnResults(
				jaxbNamedQuery.getColumnResult(),
				xmlDocumentContext
		) );


		classes( QueryProcessing.extractConstructorResults(
				jaxbNamedQuery.getConstructorResult(),
				xmlDocumentContext
		) );

		entities( QueryProcessing.extractEntityResults(
				jaxbNamedQuery.getEntityResult(),
				xmlDocumentContext
		) );
	}
}
