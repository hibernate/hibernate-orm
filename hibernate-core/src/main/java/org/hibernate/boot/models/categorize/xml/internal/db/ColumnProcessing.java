/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.xml.internal.db;

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
import org.hibernate.boot.models.categorize.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.internal.MutableAnnotationTarget;
import org.hibernate.models.internal.MutableAnnotationUsage;

import static org.hibernate.boot.models.categorize.xml.internal.XmlAnnotationHelper.applyCheckConstraints;

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

		applyColumnBasics( jaxbColumn, target, columnAnn, xmlDocumentContext );
		applyColumnNullness( jaxbColumn, target, columnAnn, xmlDocumentContext );
		applyColumnMutability( jaxbColumn, target, columnAnn, xmlDocumentContext );
		applyColumnDefinition( jaxbColumn, target, columnAnn, xmlDocumentContext );
		applyColumnUniqueness( jaxbColumn, target, columnAnn, xmlDocumentContext );
		applyColumnComment( jaxbColumn, target, columnAnn, xmlDocumentContext );
		applyCheckConstraints( jaxbColumn, target, columnAnn, xmlDocumentContext );

		if ( jaxbColumn instanceof JaxbColumnSizable sizable ) {
			applyColumnSizing( sizable, target, columnAnn, xmlDocumentContext );
		}

	}

	public static <A extends Annotation> void applyColumnDetails(
			JaxbColumnStandard jaxbColumn,
			MutableAnnotationTarget target,
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbColumn == null ) {
			return;
		}

		applyColumnBasics( jaxbColumn, target, columnAnn, xmlDocumentContext );
		applyColumnNullness( jaxbColumn, target, columnAnn, xmlDocumentContext );
		applyColumnMutability( jaxbColumn, target, columnAnn, xmlDocumentContext );
		applyColumnDefinition( jaxbColumn, target, columnAnn, xmlDocumentContext );
		applyColumnSizing( jaxbColumn, target, columnAnn, xmlDocumentContext );
		applyColumnUniqueness( jaxbColumn, target, columnAnn, xmlDocumentContext );
		applyColumnComment( jaxbColumn, target, columnAnn, xmlDocumentContext );
		applyCheckConstraints( jaxbColumn, target, columnAnn, xmlDocumentContext );
	}

	public static <A extends Annotation> void applyColumnDetails(
			JaxbColumn jaxbColumn,
			MutableAnnotationTarget target,
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbColumn == null ) {
			return;
		}

		applyColumnBasics( jaxbColumn, target, columnAnn, xmlDocumentContext );

		if ( jaxbColumn instanceof JaxbColumnNullable nullable ) {
			applyColumnNullness( nullable, target, columnAnn, xmlDocumentContext );
		}

		if ( jaxbColumn instanceof JaxbColumnMutable mutable ) {
			applyColumnMutability( mutable, target, columnAnn, xmlDocumentContext );
		}

		if ( jaxbColumn instanceof JaxbColumnDefinable definable ) {
			applyColumnDefinition( definable, target, columnAnn, xmlDocumentContext );
		}

		if ( jaxbColumn instanceof JaxbColumnSizable sizable ) {
			applyColumnSizing( sizable, target, columnAnn, xmlDocumentContext );
		}

		if ( jaxbColumn instanceof JaxbColumnUniqueable uniqueable ) {
			applyColumnUniqueness( uniqueable, target, columnAnn, xmlDocumentContext );
		}

		if ( jaxbColumn instanceof JaxbCommentable commentable ) {
			applyColumnComment( commentable, target, columnAnn, xmlDocumentContext );
		}

		if ( jaxbColumn instanceof JaxbCheckable checkable ) {
			applyCheckConstraints( checkable, target, columnAnn, xmlDocumentContext );
		}
	}

	private static <A extends Annotation> void applyColumnBasics(
			JaxbColumn jaxbColumn,
			MutableAnnotationTarget target,
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {
		if ( StringHelper.isNotEmpty( jaxbColumn.getName() ) ) {
			columnAnn.setAttributeValue( "name", jaxbColumn.getName() );
		}

		if ( StringHelper.isNotEmpty( jaxbColumn.getTable() ) ) {
			columnAnn.setAttributeValue( "table", jaxbColumn.getTable() );
		}
	}

	private static <A extends Annotation> void applyColumnNullness(
			JaxbColumnNullable jaxbColumn,
			MutableAnnotationTarget target,
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbColumn.isNullable() != null ) {
			columnAnn.setAttributeValue( "unique", jaxbColumn.isNullable() );
		}
	}

	private static <A extends Annotation> void applyColumnMutability(
			JaxbColumnMutable jaxbColumn,
			MutableAnnotationTarget target,
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbColumn.isInsertable() != null ) {
			columnAnn.setAttributeValue( "insertable", jaxbColumn.isInsertable() );
		}

		if ( jaxbColumn.isUpdatable() != null ) {
			columnAnn.setAttributeValue( "updatable", jaxbColumn.isUpdatable() );
		}
	}

	private static <A extends Annotation> void applyColumnSizing(
			JaxbColumnSizable jaxbColumn,
			MutableAnnotationTarget target,
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {

		if ( jaxbColumn.getLength() != null ) {
			columnAnn.setAttributeValue( "length", jaxbColumn.getLength() );
		}

		if ( jaxbColumn.getPrecision() != null ) {
			columnAnn.setAttributeValue( "precision", jaxbColumn.getPrecision() );
		}

		if ( jaxbColumn.getScale() != null ) {
			columnAnn.setAttributeValue( "scale", jaxbColumn.getScale() );
		}
	}

	private static <A extends Annotation> void applyColumnUniqueness(
			JaxbColumnUniqueable jaxbColumn,
			MutableAnnotationTarget target,
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbColumn.isUnique() != null ) {
			columnAnn.setAttributeValue( "unique", jaxbColumn.isUnique() );
		}
	}

	private static <A extends Annotation> void applyColumnDefinition(
			JaxbColumnDefinable jaxbColumn,
			MutableAnnotationTarget target,
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {
		if ( StringHelper.isNotEmpty( jaxbColumn.getColumnDefinition() ) ) {
			columnAnn.setAttributeValue( "columnDefinition", jaxbColumn.getColumnDefinition() );
		}

		if ( jaxbColumn.getOptions() != null ) {
			columnAnn.setAttributeValue( "options", jaxbColumn.getOptions() );
		}
	}

	private static <A extends Annotation> void applyColumnComment(
			JaxbCommentable jaxbColumn,
			MutableAnnotationTarget target,
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {
		if ( StringHelper.isNotEmpty( jaxbColumn.getComment() ) ) {
			columnAnn.setAttributeValue( "comment", jaxbColumn.getComment() );
		}
	}
}
