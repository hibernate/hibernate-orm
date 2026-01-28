/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import jakarta.persistence.NamedStatement;
import jakarta.persistence.NamedStatements;
import org.hibernate.models.spi.ModelsContext;

import java.lang.annotation.Annotation;
import java.util.Map;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NamedStatementsJpaAnnotation implements NamedStatements {
	private NamedStatement[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public NamedStatementsJpaAnnotation(ModelsContext modelContext) {
		this.value = new NamedStatement[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NamedStatementsJpaAnnotation(NamedStatements annotation, ModelsContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NamedStatementsJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (NamedStatement[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return NamedStatements.class;
	}

	@Override
	public NamedStatement[] value() {
		return value;
	}

	public void value(NamedStatement[] value) {
		this.value = value;
	}


	// todo (jpa4) : hook in the xsd

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
