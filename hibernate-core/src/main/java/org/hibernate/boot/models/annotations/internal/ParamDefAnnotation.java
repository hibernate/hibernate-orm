/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.ParamDef;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ParamDefAnnotation implements ParamDef {
	private String name;
	private java.lang.Class<?> type;
	private java.lang.Class<? extends java.util.function.Supplier> resolver;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ParamDefAnnotation(SourceModelBuildingContext modelContext) {
		this.resolver = java.util.function.Supplier.class;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ParamDefAnnotation(ParamDef annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJdkValue( annotation, HibernateAnnotations.PARAM_DEF, "name", modelContext );
		this.type = extractJdkValue( annotation, HibernateAnnotations.PARAM_DEF, "type", modelContext );
		this.resolver = extractJdkValue( annotation, HibernateAnnotations.PARAM_DEF, "resolver", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ParamDefAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, HibernateAnnotations.PARAM_DEF, "name", modelContext );
		this.type = extractJandexValue( annotation, HibernateAnnotations.PARAM_DEF, "type", modelContext );
		this.resolver = extractJandexValue( annotation, HibernateAnnotations.PARAM_DEF, "resolver", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ParamDef.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public java.lang.Class<?> type() {
		return type;
	}

	public void type(java.lang.Class<?> value) {
		this.type = value;
	}


	@Override
	public java.lang.Class<? extends java.util.function.Supplier> resolver() {
		return resolver;
	}

	public void resolver(java.lang.Class<? extends java.util.function.Supplier> value) {
		this.resolver = value;
	}


}
