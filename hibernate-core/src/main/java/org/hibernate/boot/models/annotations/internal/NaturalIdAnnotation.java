/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.NaturalId;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NaturalIdAnnotation implements NaturalId {
	private boolean mutable;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public NaturalIdAnnotation(SourceModelBuildingContext modelContext) {
		this.mutable = false;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NaturalIdAnnotation(NaturalId annotation, SourceModelBuildingContext modelContext) {
		this.mutable = annotation.mutable();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NaturalIdAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.mutable = (boolean) attributeValues.get( "mutable" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return NaturalId.class;
	}

	@Override
	public boolean mutable() {
		return mutable;
	}

	public void mutable(boolean value) {
		this.mutable = value;
	}


}
