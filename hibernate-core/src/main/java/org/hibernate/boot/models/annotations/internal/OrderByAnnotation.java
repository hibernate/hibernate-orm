/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.OrderBy;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class OrderByAnnotation implements OrderBy {
	private String clause;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OrderByAnnotation(SourceModelBuildingContext modelContext) {
		this.clause = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OrderByAnnotation(OrderBy annotation, SourceModelBuildingContext modelContext) {
		clause = annotation.clause();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OrderByAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		clause = (String) attributeValues.get( "clause" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return OrderBy.class;
	}

	@Override
	public String clause() {
		return clause;
	}

	public void clause(String value) {
		this.clause = value;
	}


}
