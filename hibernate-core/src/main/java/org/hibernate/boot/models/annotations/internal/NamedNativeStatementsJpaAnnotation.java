/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import jakarta.persistence.NamedNativeStatement;
import jakarta.persistence.NamedNativeStatements;
import org.hibernate.models.spi.ModelsContext;

import java.lang.annotation.Annotation;
import java.util.Map;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NamedNativeStatementsJpaAnnotation implements NamedNativeStatements {
	private NamedNativeStatement[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public NamedNativeStatementsJpaAnnotation(ModelsContext modelContext) {
		this.value = new NamedNativeStatement[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NamedNativeStatementsJpaAnnotation(NamedNativeStatements annotation, ModelsContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NamedNativeStatementsJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (NamedNativeStatement[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return NamedNativeStatements.class;
	}

	@Override
	public NamedNativeStatement[] value() {
		return value;
	}

	public void value(NamedNativeStatement[] value) {
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
