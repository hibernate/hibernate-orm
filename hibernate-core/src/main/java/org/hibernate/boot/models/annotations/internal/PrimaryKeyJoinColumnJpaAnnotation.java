/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.boot.jaxb.mapping.spi.JaxbPrimaryKeyJoinColumnImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.spi.ColumnDetails;
import org.hibernate.boot.models.xml.internal.db.ForeignKeyProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import jakarta.persistence.PrimaryKeyJoinColumn;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class PrimaryKeyJoinColumnJpaAnnotation implements PrimaryKeyJoinColumn, ColumnDetails, ColumnDetails.Definable {
	private String name;
	private String referencedColumnName;
	private String columnDefinition;
	private String options;
	private jakarta.persistence.ForeignKey foreignKey;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public PrimaryKeyJoinColumnJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.name = "";
		this.referencedColumnName = "";
		this.columnDefinition = "";
		this.options = "";
		this.foreignKey = modelContext.getAnnotationDescriptorRegistry()
				.getDescriptor( jakarta.persistence.ForeignKey.class )
				.createUsage( modelContext );
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public PrimaryKeyJoinColumnJpaAnnotation(PrimaryKeyJoinColumn annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJdkValue( annotation, JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN, "name", modelContext );
		this.referencedColumnName = extractJdkValue(
				annotation,
				JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN,
				"referencedColumnName",
				modelContext
		);
		this.columnDefinition = extractJdkValue(
				annotation,
				JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN,
				"columnDefinition",
				modelContext
		);
		this.options = extractJdkValue( annotation, JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN, "options", modelContext );
		this.foreignKey = extractJdkValue(
				annotation,
				JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN,
				"foreignKey",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public PrimaryKeyJoinColumnJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN, "name", modelContext );
		this.referencedColumnName = extractJandexValue(
				annotation,
				JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN,
				"referencedColumnName",
				modelContext
		);
		this.columnDefinition = extractJandexValue(
				annotation,
				JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN,
				"columnDefinition",
				modelContext
		);
		this.options = extractJandexValue(
				annotation,
				JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN,
				"options",
				modelContext
		);
		this.foreignKey = extractJandexValue(
				annotation,
				JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN,
				"foreignKey",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return PrimaryKeyJoinColumn.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public String referencedColumnName() {
		return referencedColumnName;
	}

	public void referencedColumnName(String value) {
		this.referencedColumnName = value;
	}


	@Override
	public String columnDefinition() {
		return columnDefinition;
	}

	public void columnDefinition(String value) {
		this.columnDefinition = value;
	}


	@Override
	public String options() {
		return options;
	}

	public void options(String value) {
		this.options = value;
	}


	@Override
	public jakarta.persistence.ForeignKey foreignKey() {
		return foreignKey;
	}

	public void foreignKey(jakarta.persistence.ForeignKey value) {
		this.foreignKey = value;
	}


	public void apply(JaxbPrimaryKeyJoinColumnImpl jaxbColumn, XmlDocumentContext xmlDocumentContext) {
		if ( StringHelper.isNotEmpty( jaxbColumn.getName() ) ) {
			name( jaxbColumn.getName() );
		}

		if ( StringHelper.isNotEmpty( jaxbColumn.getColumnDefinition() ) ) {
			columnDefinition( jaxbColumn.getColumnDefinition() );
		}
		if ( StringHelper.isNotEmpty( jaxbColumn.getOptions() ) ) {
			options( jaxbColumn.getOptions() );
		}

		if ( StringHelper.isNotEmpty( jaxbColumn.getReferencedColumnName() ) ) {
			referencedColumnName( jaxbColumn.getReferencedColumnName() );
		}

		if ( jaxbColumn.getForeignKey() != null ) {
			foreignKey( ForeignKeyProcessing.createNestedForeignKeyAnnotation(
					jaxbColumn.getForeignKey(),
					xmlDocumentContext
			) );
		}

	}
}
