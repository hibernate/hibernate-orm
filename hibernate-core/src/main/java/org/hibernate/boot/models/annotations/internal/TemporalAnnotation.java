/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Temporal;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class TemporalAnnotation implements Temporal {
	private String starting;
	private String ending;
	private int secondPrecision;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public TemporalAnnotation(ModelsContext modelContext) {
		this.starting = "starting";
		this.ending = "ending";
		this.secondPrecision = -1;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public TemporalAnnotation(Temporal annotation, ModelsContext modelContext) {
		this.starting = annotation.starting();
		this.ending = annotation.ending();
		this.secondPrecision = annotation.secondPrecision();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public TemporalAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.starting = (String) attributeValues.get( "starting" );
		this.ending = (String) attributeValues.get( "ending" );
		final Integer secondPrecision = (Integer) attributeValues.get( "secondPrecision" );
		this.secondPrecision = secondPrecision == null ? -1 : secondPrecision;
		if ( this.starting == null ) {
			this.starting = "starting";
		}
		if ( this.ending == null ) {
			this.ending = "ending";
		}
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Temporal.class;
	}

	@Override
	public String starting() {
		return starting;
	}

	public void setStarting(String starting) {
		this.starting = starting;
	}

	@Override
	public String ending() {
		return ending;
	}

	public void setEnding(String ending) {
		this.ending = ending;
	}

	@Override
	public int secondPrecision() {
		return secondPrecision;
	}

	public void setSecondPrecision(int secondPrecision) {
		this.secondPrecision = secondPrecision;
	}
}
