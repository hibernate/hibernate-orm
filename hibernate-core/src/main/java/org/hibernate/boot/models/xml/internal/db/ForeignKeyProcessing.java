/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal.db;

import org.hibernate.boot.jaxb.mapping.spi.JaxbForeignKeyImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.MutableAnnotationUsage;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.ForeignKey;

/**
 * @author Steve Ebersole
 */
public class ForeignKeyProcessing {

	public static void applyForeignKey(
			JaxbForeignKeyImpl jaxbForeignKey,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbForeignKey == null ) {
			return;
		}

		createForeignKeyAnnotation( jaxbForeignKey, memberDetails, xmlDocumentContext );
	}

	public static MutableAnnotationUsage<ForeignKey> createForeignKeyAnnotation(
			JaxbForeignKeyImpl jaxbForeignKey,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbForeignKey == null ) {
			return null;
		}
		return memberDetails.applyAnnotationUsage(
				JpaAnnotations.FOREIGN_KEY,
				(foreignKeyUsage) -> {
					XmlAnnotationHelper.applyOptionalAttribute( foreignKeyUsage, "name", jaxbForeignKey.getName() );
					XmlAnnotationHelper.applyOptionalAttribute( foreignKeyUsage, "value", jaxbForeignKey.getConstraintMode() );
					XmlAnnotationHelper.applyOptionalAttribute( foreignKeyUsage, "foreignKeyDefinition", jaxbForeignKey.getForeignKeyDefinition() );
					XmlAnnotationHelper.applyOptionalAttribute( foreignKeyUsage, "options", jaxbForeignKey.getOptions() );
				},
				xmlDocumentContext.getModelBuildingContext()
		);
	}

	public static MutableAnnotationUsage<ForeignKey> createNestedForeignKeyAnnotation(
			JaxbForeignKeyImpl jaxbForeignKey,
			AnnotationTarget annotationTarget,
			XmlDocumentContext xmlDocumentContext) {
		assert jaxbForeignKey != null;

		final MutableAnnotationUsage<ForeignKey> foreignKeyUsage = JpaAnnotations.FOREIGN_KEY.createUsage(
				annotationTarget,
				xmlDocumentContext.getModelBuildingContext()
		);

		XmlAnnotationHelper.applyOptionalAttribute( foreignKeyUsage, "name", jaxbForeignKey.getName() );
		XmlAnnotationHelper.applyOptionalAttribute( foreignKeyUsage, "value", jaxbForeignKey.getConstraintMode() );
		XmlAnnotationHelper.applyOptionalAttribute( foreignKeyUsage, "foreignKeyDefinition", jaxbForeignKey.getForeignKeyDefinition() );
		XmlAnnotationHelper.applyOptionalAttribute( foreignKeyUsage, "options", jaxbForeignKey.getOptions() );

		return foreignKeyUsage;
	}
}
