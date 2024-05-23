/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SqlFragmentAliasAnnotation implements SqlFragmentAlias {
	private String alias;
	private String table;
	private java.lang.Class<?> entity;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SqlFragmentAliasAnnotation(SourceModelBuildingContext modelContext) {
		this.table = "";
		this.entity = void.class;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SqlFragmentAliasAnnotation(SqlFragmentAlias annotation, SourceModelBuildingContext modelContext) {
		this.alias = extractJdkValue( annotation, HibernateAnnotations.SQL_FRAGMENT_ALIAS, "alias", modelContext );
		this.table = extractJdkValue( annotation, HibernateAnnotations.SQL_FRAGMENT_ALIAS, "table", modelContext );
		this.entity = extractJdkValue( annotation, HibernateAnnotations.SQL_FRAGMENT_ALIAS, "entity", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SqlFragmentAliasAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.alias = extractJandexValue( annotation, HibernateAnnotations.SQL_FRAGMENT_ALIAS, "alias", modelContext );
		this.table = extractJandexValue( annotation, HibernateAnnotations.SQL_FRAGMENT_ALIAS, "table", modelContext );
		this.entity = extractJandexValue( annotation, HibernateAnnotations.SQL_FRAGMENT_ALIAS, "entity", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SqlFragmentAlias.class;
	}

	@Override
	public String alias() {
		return alias;
	}

	public void alias(String value) {
		this.alias = value;
	}


	@Override
	public String table() {
		return table;
	}

	public void table(String value) {
		this.table = value;
	}


	@Override
	public java.lang.Class<?> entity() {
		return entity;
	}

	public void entity(java.lang.Class<?> value) {
		this.entity = value;
	}


}
