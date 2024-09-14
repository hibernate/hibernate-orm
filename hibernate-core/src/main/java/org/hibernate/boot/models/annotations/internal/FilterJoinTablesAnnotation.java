/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.FilterJoinTables;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.SourceModelBuildingContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class FilterJoinTablesAnnotation implements FilterJoinTables, RepeatableContainer<FilterJoinTable> {
	private org.hibernate.annotations.FilterJoinTable[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public FilterJoinTablesAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public FilterJoinTablesAnnotation(FilterJoinTables annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue( annotation, HibernateAnnotations.FILTER_JOIN_TABLES, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public FilterJoinTablesAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.value = (FilterJoinTable[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return FilterJoinTables.class;
	}

	@Override
	public org.hibernate.annotations.FilterJoinTable[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.FilterJoinTable[] value) {
		this.value = value;
	}


}
