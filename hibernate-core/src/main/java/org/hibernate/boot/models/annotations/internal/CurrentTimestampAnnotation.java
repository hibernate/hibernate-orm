/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.models.spi.SourceModelBuildingContext;

import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventType.UPDATE;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CurrentTimestampAnnotation implements CurrentTimestamp {
	private org.hibernate.generator.EventType[] event;
	private org.hibernate.annotations.SourceType source;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CurrentTimestampAnnotation(SourceModelBuildingContext modelContext) {
		this.event = new org.hibernate.generator.EventType[] {INSERT, UPDATE};
		this.source = org.hibernate.annotations.SourceType.DB;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CurrentTimestampAnnotation(CurrentTimestamp annotation, SourceModelBuildingContext modelContext) {
		this.event = annotation.event();
		this.source = annotation.source();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CurrentTimestampAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.event = (org.hibernate.generator.EventType[]) attributeValues.get( "event" );
		this.source = (org.hibernate.annotations.SourceType) attributeValues.get( "source" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return CurrentTimestamp.class;
	}

	@Override
	public org.hibernate.generator.EventType[] event() {
		return event;
	}

	public void event(org.hibernate.generator.EventType[] value) {
		this.event = value;
	}

	@Override
	public org.hibernate.annotations.SourceType source() {
		return source;
	}

	public void source(org.hibernate.annotations.SourceType value) {
		this.source = value;
	}


}
