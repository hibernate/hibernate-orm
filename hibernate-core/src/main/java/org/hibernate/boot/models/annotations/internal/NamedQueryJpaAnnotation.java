/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedQueryImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.xml.internal.QueryProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import jakarta.persistence.NamedQuery;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
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
	public NamedQueryJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.resultClass = void.class;
		this.lockMode = jakarta.persistence.LockModeType.NONE;
		this.hints = new jakarta.persistence.QueryHint[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NamedQueryJpaAnnotation(NamedQuery annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJdkValue( annotation, JpaAnnotations.NAMED_QUERY, "name", modelContext );
		this.query = extractJdkValue( annotation, JpaAnnotations.NAMED_QUERY, "query", modelContext );
		this.resultClass = extractJdkValue( annotation, JpaAnnotations.NAMED_QUERY, "resultClass", modelContext );
		this.lockMode = extractJdkValue( annotation, JpaAnnotations.NAMED_QUERY, "lockMode", modelContext );
		this.hints = extractJdkValue( annotation, JpaAnnotations.NAMED_QUERY, "hints", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NamedQueryJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, JpaAnnotations.NAMED_QUERY, "name", modelContext );
		this.query = extractJandexValue( annotation, JpaAnnotations.NAMED_QUERY, "query", modelContext );
		this.resultClass = extractJandexValue( annotation, JpaAnnotations.NAMED_QUERY, "resultClass", modelContext );
		this.lockMode = extractJandexValue( annotation, JpaAnnotations.NAMED_QUERY, "lockMode", modelContext );
		this.hints = extractJandexValue( annotation, JpaAnnotations.NAMED_QUERY, "hints", modelContext );
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


	public void apply(JaxbNamedQueryImpl jaxbNamedQuery, XmlDocumentContext xmlDocumentContext) {
		name( jaxbNamedQuery.getName() );
		query( jaxbNamedQuery.getQuery() );
		lockMode( coalesce( jaxbNamedQuery.getLockMode(), jakarta.persistence.LockModeType.NONE ) );

		hints( QueryProcessing.collectQueryHints( jaxbNamedQuery.getHints(), xmlDocumentContext ) );
	}
}
