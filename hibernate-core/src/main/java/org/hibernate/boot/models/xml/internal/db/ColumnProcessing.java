/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal.db;

import java.lang.annotation.Annotation;

import org.hibernate.boot.jaxb.mapping.spi.db.JaxbCheckable;
import org.hibernate.boot.jaxb.mapping.spi.db.JaxbColumn;
import org.hibernate.boot.jaxb.mapping.spi.db.JaxbColumnCommon;
import org.hibernate.boot.jaxb.mapping.spi.db.JaxbColumnDefinable;
import org.hibernate.boot.jaxb.mapping.spi.db.JaxbColumnMutable;
import org.hibernate.boot.jaxb.mapping.spi.db.JaxbColumnNullable;
import org.hibernate.boot.jaxb.mapping.spi.db.JaxbColumnSizable;
import org.hibernate.boot.jaxb.mapping.spi.db.JaxbColumnStandard;
import org.hibernate.boot.jaxb.mapping.spi.db.JaxbColumnUniqueable;
import org.hibernate.boot.jaxb.mapping.spi.db.JaxbCommentable;
import org.hibernate.boot.models.internal.AnnotationUsageHelper;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.models.spi.MutableAnnotationTarget;
import org.hibernate.models.spi.MutableAnnotationUsage;

/**
 * @author Steve Ebersole
 */
public class ColumnProcessing {

	public static <A extends Annotation> void applyColumnDetails(
			JaxbColumnCommon jaxbColumn,
			MutableAnnotationTarget target,
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbColumn == null ) {
			return;
		}

		applyColumnBasics( jaxbColumn, columnAnn, xmlDocumentContext );
		applyColumnNullness( jaxbColumn, columnAnn, xmlDocumentContext );
		applyColumnMutability( jaxbColumn, columnAnn, xmlDocumentContext );
		applyColumnDefinition( jaxbColumn, columnAnn, xmlDocumentContext );
		applyColumnUniqueness( jaxbColumn, columnAnn, xmlDocumentContext );
		applyColumnComment( jaxbColumn, columnAnn, xmlDocumentContext );
		XmlAnnotationHelper.applyCheckConstraints( jaxbColumn, target, columnAnn, xmlDocumentContext );

		if ( jaxbColumn instanceof JaxbColumnSizable sizable ) {
			applyColumnSizing( sizable, columnAnn, xmlDocumentContext );
		}

	}

	public static <A extends Annotation> void applyColumnDetails(
			JaxbColumnCommon jaxbColumn,
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbColumn == null ) {
			return;
		}

		applyColumnBasics( jaxbColumn, columnAnn, xmlDocumentContext );
		applyColumnNullness( jaxbColumn, columnAnn, xmlDocumentContext );
		applyColumnMutability( jaxbColumn, columnAnn, xmlDocumentContext );
		applyColumnDefinition( jaxbColumn, columnAnn, xmlDocumentContext );
		applyColumnUniqueness( jaxbColumn, columnAnn, xmlDocumentContext );
		applyColumnComment( jaxbColumn, columnAnn, xmlDocumentContext );
		XmlAnnotationHelper.applyCheckConstraints( jaxbColumn, columnAnn, xmlDocumentContext );

		if ( jaxbColumn instanceof JaxbColumnSizable sizable ) {
			applyColumnSizing( sizable, columnAnn, xmlDocumentContext );
		}
	}

	public static <A extends Annotation> void applyColumnDetails(
			JaxbColumnStandard jaxbColumn,
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbColumn == null ) {
			return;
		}

		applyColumnBasics( jaxbColumn, columnAnn, xmlDocumentContext );
		applyColumnNullness( jaxbColumn, columnAnn, xmlDocumentContext );
		applyColumnMutability( jaxbColumn, columnAnn, xmlDocumentContext );
		applyColumnDefinition( jaxbColumn, columnAnn, xmlDocumentContext );
		applyColumnSizing( jaxbColumn, columnAnn, xmlDocumentContext );
		applyColumnUniqueness( jaxbColumn, columnAnn, xmlDocumentContext );
		applyColumnComment( jaxbColumn, columnAnn, xmlDocumentContext );
		XmlAnnotationHelper.applyCheckConstraints( jaxbColumn, columnAnn, xmlDocumentContext );
	}

	public static <A extends Annotation> void applyColumnDetails(
			JaxbColumn jaxbColumn,
			MutableAnnotationTarget target,
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbColumn == null ) {
			return;
		}

		applyColumnBasics( jaxbColumn, columnAnn, xmlDocumentContext );

		if ( jaxbColumn instanceof JaxbColumnNullable nullable ) {
			applyColumnNullness( nullable, columnAnn, xmlDocumentContext );
		}

		if ( jaxbColumn instanceof JaxbColumnMutable mutable ) {
			applyColumnMutability( mutable, columnAnn, xmlDocumentContext );
		}

		if ( jaxbColumn instanceof JaxbColumnDefinable definable ) {
			applyColumnDefinition( definable, columnAnn, xmlDocumentContext );
		}

		if ( jaxbColumn instanceof JaxbColumnSizable sizable ) {
			applyColumnSizing( sizable, columnAnn, xmlDocumentContext );
		}

		if ( jaxbColumn instanceof JaxbColumnUniqueable uniqueable ) {
			applyColumnUniqueness( uniqueable, columnAnn, xmlDocumentContext );
		}

		if ( jaxbColumn instanceof JaxbCommentable commentable ) {
			applyColumnComment( commentable, columnAnn, xmlDocumentContext );
		}

		if ( jaxbColumn instanceof JaxbCheckable checkable ) {
			XmlAnnotationHelper.applyCheckConstraints( checkable, target, columnAnn, xmlDocumentContext );
		}
	}

	private static <A extends Annotation> void applyColumnBasics(
			JaxbColumn jaxbColumn,
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {
		AnnotationUsageHelper.applyStringAttributeIfSpecified( "name", jaxbColumn.getName(), columnAnn );

		AnnotationUsageHelper.applyStringAttributeIfSpecified( "table", jaxbColumn.getTable(), columnAnn );
	}

	private static <A extends Annotation> void applyColumnNullness(
			JaxbColumnNullable jaxbColumn,
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {
		XmlProcessingHelper.applyAttributeIfSpecified( "nullable", jaxbColumn.isNullable(), columnAnn );
	}

	private static <A extends Annotation> void applyColumnMutability(
			JaxbColumnMutable jaxbColumn,
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {
		XmlProcessingHelper.applyAttributeIfSpecified( "insertable", jaxbColumn.isInsertable(), columnAnn );

		XmlProcessingHelper.applyAttributeIfSpecified( "updatable", jaxbColumn.isUpdatable(), columnAnn );
	}

	private static <A extends Annotation> void applyColumnSizing(
			JaxbColumnSizable jaxbColumn,
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {
		XmlProcessingHelper.applyAttributeIfSpecified( "length", jaxbColumn.getLength(), columnAnn );

		XmlProcessingHelper.applyAttributeIfSpecified( "precision", jaxbColumn.getPrecision(), columnAnn );

		XmlProcessingHelper.applyAttributeIfSpecified( "scale", jaxbColumn.getScale(), columnAnn );
	}

	private static <A extends Annotation> void applyColumnUniqueness(
			JaxbColumnUniqueable jaxbColumn,
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {
		XmlProcessingHelper.applyAttributeIfSpecified( "unique", jaxbColumn.isUnique(), columnAnn );
	}

	private static <A extends Annotation> void applyColumnDefinition(
			JaxbColumnDefinable jaxbColumn,
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {
		XmlProcessingHelper.applyAttributeIfSpecified(
				"columnDefinition",
				jaxbColumn.getColumnDefinition(),
				columnAnn
		);

		XmlProcessingHelper.applyAttributeIfSpecified( "options", jaxbColumn.getOptions(), columnAnn );
	}

	private static <A extends Annotation> void applyColumnComment(
			JaxbCommentable jaxbColumn,
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {
		XmlProcessingHelper.applyAttributeIfSpecified( "comment", jaxbColumn.getComment(), columnAnn );
	}
}
