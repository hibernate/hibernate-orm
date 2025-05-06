/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.models.spi.ModelsContext;


import jakarta.persistence.AssociationOverride;

import static org.hibernate.boot.models.JpaAnnotations.ASSOCIATION_OVERRIDE;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AssociationOverrideJpaAnnotation implements AssociationOverride {

	private String name;
	private jakarta.persistence.JoinColumn[] joinColumns;
	private jakarta.persistence.ForeignKey foreignKey;
	private jakarta.persistence.JoinTable joinTable;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AssociationOverrideJpaAnnotation(ModelsContext modelContext) {
		this.joinColumns = new jakarta.persistence.JoinColumn[0];
		this.foreignKey = JpaAnnotations.FOREIGN_KEY.createUsage( modelContext );
		this.joinTable = JpaAnnotations.JOIN_TABLE.createUsage( modelContext );
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AssociationOverrideJpaAnnotation(AssociationOverride annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.joinColumns = extractJdkValue( annotation, ASSOCIATION_OVERRIDE, "joinColumns", modelContext );
		this.foreignKey = extractJdkValue( annotation, ASSOCIATION_OVERRIDE, "foreignKey", modelContext );
		this.joinTable = extractJdkValue( annotation, ASSOCIATION_OVERRIDE, "joinTable", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AssociationOverrideJpaAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.joinColumns = (jakarta.persistence.JoinColumn[]) attributeValues.get( "joinColumns" );
		this.foreignKey = (jakarta.persistence.ForeignKey) attributeValues.get( "foreignKey" );
		this.joinTable = (jakarta.persistence.JoinTable) attributeValues.get( "joinTable" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return AssociationOverride.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public jakarta.persistence.JoinColumn[] joinColumns() {
		return joinColumns;
	}

	public void joinColumns(jakarta.persistence.JoinColumn[] value) {
		this.joinColumns = value;
	}


	@Override
	public jakarta.persistence.ForeignKey foreignKey() {
		return foreignKey;
	}

	public void foreignKey(jakarta.persistence.ForeignKey value) {
		this.foreignKey = value;
	}


	@Override
	public jakarta.persistence.JoinTable joinTable() {
		return joinTable;
	}

	public void joinTable(jakarta.persistence.JoinTable value) {
		this.joinTable = value;
	}


}
