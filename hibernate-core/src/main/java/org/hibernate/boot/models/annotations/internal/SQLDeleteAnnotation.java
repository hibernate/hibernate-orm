/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.CustomSqlDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SQLDeleteAnnotation implements SQLDelete, CustomSqlDetails {
	private String sql;
	private boolean callable;
	private java.lang.Class<? extends org.hibernate.jdbc.Expectation> verify;
	private org.hibernate.annotations.ResultCheckStyle check;
	private String table;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SQLDeleteAnnotation(SourceModelBuildingContext modelContext) {
		this.callable = false;
		this.verify = org.hibernate.jdbc.Expectation.class;
		this.check = org.hibernate.annotations.ResultCheckStyle.NONE;
		this.table = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SQLDeleteAnnotation(SQLDelete annotation, SourceModelBuildingContext modelContext) {
		this.sql = annotation.sql();
		this.callable = annotation.callable();
		this.verify = annotation.verify();
		this.check = annotation.check();
		this.table = annotation.table();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SQLDeleteAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.sql = extractJandexValue( annotation, HibernateAnnotations.SQL_DELETE, "sql", modelContext );
		this.callable = extractJandexValue( annotation, HibernateAnnotations.SQL_DELETE, "callable", modelContext );
		this.verify = extractJandexValue( annotation, HibernateAnnotations.SQL_DELETE, "verify", modelContext );
		this.check = extractJandexValue( annotation, HibernateAnnotations.SQL_DELETE, "check", modelContext );
		this.table = extractJandexValue( annotation, HibernateAnnotations.SQL_DELETE, "table", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SQLDelete.class;
	}

	@Override
	public String sql() {
		return sql;
	}

	public void sql(String value) {
		this.sql = value;
	}


	@Override
	public boolean callable() {
		return callable;
	}

	public void callable(boolean value) {
		this.callable = value;
	}


	@Override
	public java.lang.Class<? extends org.hibernate.jdbc.Expectation> verify() {
		return verify;
	}

	public void verify(java.lang.Class<? extends org.hibernate.jdbc.Expectation> value) {
		this.verify = value;
	}


	@Override
	public org.hibernate.annotations.ResultCheckStyle check() {
		return check;
	}

	public void check(org.hibernate.annotations.ResultCheckStyle value) {
		this.check = value;
	}


	@Override
	public String table() {
		return table;
	}

	public void table(String value) {
		this.table = value;
	}


}
