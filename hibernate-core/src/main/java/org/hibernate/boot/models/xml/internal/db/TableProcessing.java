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
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.MutableAnnotationUsage;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.JoinTable;

/**
 * @author Steve Ebersole
 */
public class TableProcessing {
	public static MutableAnnotationUsage<JoinTable> applyJoinTable(
			JaxbJoinTableImpl jaxbJoinTable,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbJoinTable == null ) {
			return null;
		}

		final MutableAnnotationUsage<JoinTable> joinTableUsage = memberDetails.applyAnnotationUsage(
				JpaAnnotations.JOIN_TABLE,
				xmlDocumentContext.getModelBuildingContext()
		);

		XmlAnnotationHelper.applyOptionalAttribute( joinTableUsage, "name", jaxbJoinTable.getName() );
		XmlAnnotationHelper.applyTableAttributes( jaxbJoinTable, memberDetails, joinTableUsage, JpaAnnotations.JOIN_TABLE, xmlDocumentContext );

		final List<JaxbJoinColumnImpl> joinColumns = jaxbJoinTable.getJoinColumn();
		if ( CollectionHelper.isNotEmpty( joinColumns ) ) {
			joinTableUsage.setAttributeValue( "joinColumns", JoinColumnProcessing.transformJoinColumnList( joinColumns, xmlDocumentContext ) );
		}

		final List<JaxbJoinColumnImpl> inverseJoinColumns = jaxbJoinTable.getInverseJoinColumn();
		if ( CollectionHelper.isNotEmpty( inverseJoinColumns ) ) {
			joinTableUsage.setAttributeValue( "inverseJoinColumns", JoinColumnProcessing.transformJoinColumnList( inverseJoinColumns, xmlDocumentContext ) );
		}

		if ( jaxbJoinTable.getForeignKey() != null ) {
			joinTableUsage.setAttributeValue(
					"foreignKey",
					ForeignKeyProcessing.createNestedForeignKeyAnnotation( jaxbJoinTable.getForeignKey(), memberDetails, xmlDocumentContext )
			);
		}
		if ( jaxbJoinTable.getInverseForeignKey() != null ) {
			joinTableUsage.setAttributeValue(
					"inverseForeignKey",
					ForeignKeyProcessing.createNestedForeignKeyAnnotation( jaxbJoinTable.getInverseForeignKey(), memberDetails, xmlDocumentContext )
			);
		}

		return joinTableUsage;
	}
}
