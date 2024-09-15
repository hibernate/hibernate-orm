/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.SQLSelect;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import static org.hibernate.boot.models.HibernateAnnotations.SQL_SELECT;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SQLSelectAnnotation implements SQLSelect {

	private String sql;
	private jakarta.persistence.SqlResultSetMapping resultSetMapping;
	private String[] querySpaces;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SQLSelectAnnotation(SourceModelBuildingContext modelContext) {
		this.resultSetMapping = JpaAnnotations.SQL_RESULT_SET_MAPPING.createUsage( modelContext );
		this.querySpaces = new String[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SQLSelectAnnotation(SQLSelect annotation, SourceModelBuildingContext modelContext) {
		this.sql = annotation.sql();
		this.resultSetMapping = extractJdkValue( annotation, SQL_SELECT, "resultSetMapping", modelContext );
		this.querySpaces = annotation.querySpaces();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SQLSelectAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.sql = (String) attributeValues.get( "sql" );
		this.resultSetMapping = (jakarta.persistence.SqlResultSetMapping) attributeValues.get( "resultSetMapping" );
		this.querySpaces = (String[]) attributeValues.get( "querySpaces" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SQLSelect.class;
	}

	@Override
	public String sql() {
		return sql;
	}

	public void sql(String value) {
		this.sql = value;
	}


	@Override
	public jakarta.persistence.SqlResultSetMapping resultSetMapping() {
		return resultSetMapping;
	}

	public void resultSetMapping(jakarta.persistence.SqlResultSetMapping value) {
		this.resultSetMapping = value;
	}


	@Override
	public String[] querySpaces() {
		return querySpaces;
	}

	public void querySpaces(String[] value) {
		this.querySpaces = value;
	}


}
