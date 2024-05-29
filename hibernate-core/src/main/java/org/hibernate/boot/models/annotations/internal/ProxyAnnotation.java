/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.Proxy;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ProxyAnnotation implements Proxy {
	private boolean lazy;
	private java.lang.Class<?> proxyClass;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ProxyAnnotation(SourceModelBuildingContext modelContext) {
		this.lazy = true;
		this.proxyClass = void.class;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ProxyAnnotation(Proxy annotation, SourceModelBuildingContext modelContext) {
		this.lazy = annotation.lazy();
		this.proxyClass = annotation.proxyClass();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ProxyAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.lazy = extractJandexValue( annotation, HibernateAnnotations.PROXY, "lazy", modelContext );
		this.proxyClass = extractJandexValue( annotation, HibernateAnnotations.PROXY, "proxyClass", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Proxy.class;
	}

	@Override
	public boolean lazy() {
		return lazy;
	}

	public void lazy(boolean value) {
		this.lazy = value;
	}


	@Override
	public java.lang.Class<?> proxyClass() {
		return proxyClass;
	}

	public void proxyClass(java.lang.Class<?> value) {
		this.proxyClass = value;
	}


}
