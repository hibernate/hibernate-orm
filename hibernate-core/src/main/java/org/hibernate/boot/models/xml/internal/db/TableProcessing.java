/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal.db;

import java.util.List;

import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinTableImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.MutableAnnotationTarget;
import org.hibernate.models.spi.MutableAnnotationUsage;

import jakarta.persistence.JoinTable;

/**
 * @author Steve Ebersole
 */
public class TableProcessing {
	public static MutableAnnotationUsage<JoinTable> transformJoinTable(
			JaxbJoinTableImpl jaxbJoinTable,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbJoinTable == null ) {
			return null;
		}

		final MutableAnnotationUsage<JoinTable> joinTableUsage = target.applyAnnotationUsage(
				JpaAnnotations.JOIN_TABLE,
				xmlDocumentContext.getModelBuildingContext()
		);
		applyJoinTable( jaxbJoinTable, joinTableUsage, target, xmlDocumentContext );
		return joinTableUsage;
	}

	public static MutableAnnotationUsage<JoinTable> createNestedJoinTable(
			JaxbJoinTableImpl jaxbJoinTable,
			AnnotationTarget annotationTarget,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbJoinTable == null ) {
			return null;
		}

		final MutableAnnotationUsage<JoinTable> joinTableAnn = XmlProcessingHelper.makeNestedAnnotation(
				JoinTable.class,
				annotationTarget,
				xmlDocumentContext
		);
		applyJoinTable( jaxbJoinTable, joinTableAnn, annotationTarget, xmlDocumentContext );
		return joinTableAnn;
	}

	private static void applyJoinTable(
			JaxbJoinTableImpl jaxbJoinTable,
			MutableAnnotationUsage<JoinTable> joinTableUsage,
			AnnotationTarget annotationTarget,
			XmlDocumentContext xmlDocumentContext) {

		XmlAnnotationHelper.applyOptionalAttribute( joinTableUsage, "name", jaxbJoinTable.getName() );
		XmlAnnotationHelper.applyTableAttributes( jaxbJoinTable, annotationTarget, joinTableUsage, JpaAnnotations.JOIN_TABLE, xmlDocumentContext );

		final List<JaxbJoinColumnImpl> joinColumns = jaxbJoinTable.getJoinColumn();
		if ( CollectionHelper.isNotEmpty( joinColumns ) ) {
			joinTableUsage.setAttributeValue( "joinColumns", JoinColumnProcessing.transformJoinColumnList(
					joinColumns,
					annotationTarget,
					xmlDocumentContext
			) );
		}
		final List<JaxbJoinColumnImpl> inverseJoinColumns = jaxbJoinTable.getInverseJoinColumn();
		if ( CollectionHelper.isNotEmpty( inverseJoinColumns ) ) {
			joinTableUsage.setAttributeValue( "inverseJoinColumns", JoinColumnProcessing.transformJoinColumnList(
					inverseJoinColumns,
					annotationTarget,
					xmlDocumentContext
			) );
		}

		if ( jaxbJoinTable.getForeignKey() != null ) {
			joinTableUsage.setAttributeValue(
					"foreignKey",
					ForeignKeyProcessing.createNestedForeignKeyAnnotation( jaxbJoinTable.getForeignKey(), annotationTarget, xmlDocumentContext )
			);
		}
		if ( jaxbJoinTable.getInverseForeignKey() != null ) {
			joinTableUsage.setAttributeValue(
					"inverseForeignKey",
					ForeignKeyProcessing.createNestedForeignKeyAnnotation( jaxbJoinTable.getInverseForeignKey(), annotationTarget, xmlDocumentContext )
			);
		}
	}
}
