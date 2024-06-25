/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import jakarta.persistence.AssociationOverride;

import static org.hibernate.boot.models.JpaAnnotations.ASSOCIATION_OVERRIDE;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
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
	public AssociationOverrideJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.joinColumns = new jakarta.persistence.JoinColumn[0];
		this.foreignKey = JpaAnnotations.FOREIGN_KEY.createUsage( modelContext );
		this.joinTable = JpaAnnotations.JOIN_TABLE.createUsage( modelContext );
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AssociationOverrideJpaAnnotation(AssociationOverride annotation, SourceModelBuildingContext modelContext) {
		this.name = annotation.name();
		this.joinColumns = extractJdkValue( annotation, ASSOCIATION_OVERRIDE, "joinColumns", modelContext );
		this.foreignKey = extractJdkValue( annotation, ASSOCIATION_OVERRIDE, "foreignKey", modelContext );
		this.joinTable = extractJdkValue( annotation, ASSOCIATION_OVERRIDE, "joinTable", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AssociationOverrideJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, ASSOCIATION_OVERRIDE, "name", modelContext );
		this.joinColumns = extractJandexValue( annotation, ASSOCIATION_OVERRIDE, "joinColumns", modelContext );
		this.foreignKey = extractJandexValue( annotation, ASSOCIATION_OVERRIDE, "foreignKey", modelContext );
		this.joinTable = extractJandexValue( annotation, ASSOCIATION_OVERRIDE, "joinTable", modelContext );
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