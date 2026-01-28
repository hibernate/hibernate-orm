/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import jakarta.persistence.NamedNativeStatement;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.models.spi.ModelsContext;

import java.lang.annotation.Annotation;
import java.util.Map;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NamedNativeStatementJpaAnnotation implements NamedNativeStatement {
	private String name;
	private String statement;
	private jakarta.persistence.QueryHint[] hints;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public NamedNativeStatementJpaAnnotation(ModelsContext modelContext) {
		this.hints = new jakarta.persistence.QueryHint[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NamedNativeStatementJpaAnnotation(NamedNativeStatement annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.statement = annotation.statement();
		this.hints = extractJdkValue( annotation, JpaAnnotations.NAMED_NATIVE_STATEMENT, "hints", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NamedNativeStatementJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.statement = (String) attributeValues.get( "statement" );
		this.hints = (jakarta.persistence.QueryHint[]) attributeValues.get( "hints" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return NamedNativeStatement.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public String statement() {
		return statement;
	}

	public void statement(String value) {
		this.statement = value;
	}


	@Override
	public jakarta.persistence.QueryHint[] hints() {
		return hints;
	}

	public void hints(jakarta.persistence.QueryHint[] value) {
		this.hints = value;
	}


	// todo (jpa4) : hook in xsd

//	public void apply(JaxbNamedHqlQueryImpl jaxbNamedQuery, XmlDocumentContext xmlDocumentContext) {
//		name( jaxbNamedQuery.getName() );
//		query( jaxbNamedQuery.getQuery() );
//
//		resultClass( TypeHelper.resolveClassReference( jaxbNamedQuery.getResultClass(), xmlDocumentContext, void.class ) );
//		entityGraph( jaxbNamedQuery.getEntityGraph() );
//
//		lockMode( coalesce( jaxbNamedQuery.getLockMode(), jakarta.persistence.LockModeType.NONE ) );
//		lockScope( coalesce( jaxbNamedQuery.getLockScope(), PessimisticLockScope.NORMAL ) );
//
//		hints( QueryProcessing.collectQueryHints( jaxbNamedQuery.getHints(), xmlDocumentContext ) );
//	}
}
