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
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
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
		if ( StringHelper.isNotEmpty( jaxbColumn.getName() ) ) {
			columnAnn.setAttributeValue( "name", jaxbColumn.getName() );
		}

		if ( StringHelper.isNotEmpty( jaxbColumn.getTable() ) ) {
			columnAnn.setAttributeValue( "table", jaxbColumn.getTable() );
		}
	}

	private static <A extends Annotation> void applyColumnNullness(
			JaxbColumnNullable jaxbColumn,
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbColumn.isNullable() != null ) {
			columnAnn.setAttributeValue( "unique", jaxbColumn.isNullable() );
		}
	}

	private static <A extends Annotation> void applyColumnMutability(
			JaxbColumnMutable jaxbColumn,
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
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbColumn.isUnique() != null ) {
			columnAnn.setAttributeValue( "unique", jaxbColumn.isUnique() );
		}
	}

	private static <A extends Annotation> void applyColumnDefinition(
			JaxbColumnDefinable jaxbColumn,
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
			MutableAnnotationUsage<A> columnAnn,
			XmlDocumentContext xmlDocumentContext) {
		if ( StringHelper.isNotEmpty( jaxbColumn.getComment() ) ) {
			columnAnn.setAttributeValue( "comment", jaxbColumn.getComment() );
		}
	}
}
