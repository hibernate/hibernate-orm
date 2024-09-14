/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class DiscriminatorOptionsAnnotation implements DiscriminatorOptions {
	private boolean force;
	private boolean insert;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public DiscriminatorOptionsAnnotation(SourceModelBuildingContext modelContext) {
		this.force = false;
		this.insert = true;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public DiscriminatorOptionsAnnotation(DiscriminatorOptions annotation, SourceModelBuildingContext modelContext) {
		this.force = annotation.force();
		this.insert = annotation.insert();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public DiscriminatorOptionsAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.force = (boolean) attributeValues.get( "force");
		this.insert = (boolean) attributeValues.get( "insert");
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DiscriminatorOptions.class;
	}

	@Override
	public boolean force() {
		return force;
	}

	public void force(boolean value) {
		this.force = value;
	}


	@Override
	public boolean insert() {
		return insert;
	}

	public void insert(boolean value) {
		this.insert = value;
	}


}
