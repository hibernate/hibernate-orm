/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.boot.jaxb.mapping.spi.JaxbFilterImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.FilterDetails;
import org.hibernate.boot.models.xml.internal.FilterProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class FilterAnnotation implements Filter, FilterDetails {
	private String name;
	private String condition;
	private boolean deduceAliasInjectionPoints;
	private SqlFragmentAlias[] aliases;
	private Join join;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public FilterAnnotation(ModelsContext modelContext) {
		this.condition = "";
		this.deduceAliasInjectionPoints = true;
		this.aliases = new SqlFragmentAlias[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public FilterAnnotation(Filter annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.condition = annotation.condition();
		this.deduceAliasInjectionPoints = annotation.deduceAliasInjectionPoints();
		this.aliases = extractJdkValue( annotation, HibernateAnnotations.FILTER, "aliases", modelContext );
		this.join = annotation.join();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public FilterAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.condition = (String) attributeValues.get( "condition" );
		this.deduceAliasInjectionPoints = (boolean) attributeValues.get( "deduceAliasInjectionPoints" );
		this.aliases = (SqlFragmentAlias[]) attributeValues.get( "aliases" );
		this.join = (Join) attributeValues.get( "join" );
	}


	@Override
	public Class<? extends Annotation> annotationType() {
		return Filter.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public String condition() {
		return condition;
	}

	public void condition(String value) {
		this.condition = value;
	}

	@Override
	public Join join() {
		return join;
	}

	public void join(Join join) {
		this.join = join;
	}

	@Override
	public boolean deduceAliasInjectionPoints() {
		return deduceAliasInjectionPoints;
	}

	public void deduceAliasInjectionPoints(boolean value) {
		this.deduceAliasInjectionPoints = value;
	}


	@Override
	public SqlFragmentAlias[] aliases() {
		return aliases;
	}

	public void aliases(SqlFragmentAlias[] value) {
		this.aliases = value;
	}


	@Override
	public void apply(JaxbFilterImpl jaxbFilter, XmlDocumentContext xmlDocumentContext) {
		name( jaxbFilter.getName() );

		if ( StringHelper.isNotEmpty( jaxbFilter.getCondition() ) ) {
			condition( jaxbFilter.getCondition() );
		}

		if ( jaxbFilter.isAutoAliasInjection() != null ) {
			deduceAliasInjectionPoints( jaxbFilter.isAutoAliasInjection() );
		}

		aliases( FilterProcessing.collectSqlFragmentAliases(
				jaxbFilter.getAliases(),
				xmlDocumentContext
		) );
	}
}
