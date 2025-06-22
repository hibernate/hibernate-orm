/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal.attr;

import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.BasicJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.FormulaAnnotation;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.AccessType;

import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyAccess;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyAttributeAccessor;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyFetching;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyOptimisticLock;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyOptionality;
import static org.hibernate.internal.util.NullnessHelper.coalesce;

/**
 * @author Steve Ebersole
 */
public class BasicAttributeProcessing {

	public static MutableMemberDetails processBasicAttribute(
			JaxbBasicImpl jaxbBasic,
			MutableClassDetails declarer,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		final AccessType accessType = coalesce( jaxbBasic.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbBasic.getName(),
				accessType,
				declarer
		);

		final BasicJpaAnnotation basicAnn = (BasicJpaAnnotation) memberDetails.applyAnnotationUsage(
				JpaAnnotations.BASIC,
				xmlDocumentContext.getModelBuildingContext()
		);

		applyAccess( accessType, memberDetails, xmlDocumentContext );
		applyAttributeAccessor( jaxbBasic, memberDetails, xmlDocumentContext );
		applyFetching( jaxbBasic, memberDetails, basicAnn, xmlDocumentContext );
		applyOptionality( jaxbBasic, basicAnn, xmlDocumentContext );
		applyOptimisticLock( jaxbBasic, memberDetails, xmlDocumentContext );

		if ( StringHelper.isNotEmpty( jaxbBasic.getFormula() ) ) {
			assert jaxbBasic.getColumn() == null;
			final FormulaAnnotation formulaAnn = (FormulaAnnotation) memberDetails.applyAnnotationUsage(
					HibernateAnnotations.FORMULA,
					xmlDocumentContext.getModelBuildingContext()
			);
			formulaAnn.value( jaxbBasic.getFormula() );
		}
		else if ( jaxbBasic.getColumn() != null ) {
			final ColumnJpaAnnotation columnAnn = (ColumnJpaAnnotation) memberDetails.applyAnnotationUsage(
					JpaAnnotations.COLUMN,
					xmlDocumentContext.getModelBuildingContext()
			);
			columnAnn.apply( jaxbBasic.getColumn(), xmlDocumentContext );
			XmlAnnotationHelper.applyColumnTransformation( jaxbBasic.getColumn(), memberDetails, xmlDocumentContext );
		}

		XmlAnnotationHelper.applyConvert( jaxbBasic.getConvert(), memberDetails, xmlDocumentContext );

		XmlAnnotationHelper.applyBasicTypeComposition( jaxbBasic, memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyTemporal( jaxbBasic.getTemporal(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyLob( jaxbBasic.getLob(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyEnumerated( jaxbBasic.getEnumerated(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyNationalized( jaxbBasic.getNationalized(), memberDetails, xmlDocumentContext );

		// todo : value generation
		// todo : ...

		return memberDetails;
	}
}
