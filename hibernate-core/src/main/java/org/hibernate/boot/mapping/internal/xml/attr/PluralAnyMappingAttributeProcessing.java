/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.xml.attr;

import jakarta.persistence.AccessType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralAnyMappingImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.internal.ManyToAnyAnnotation;
import org.hibernate.boot.mapping.internal.xml.XmlAnnotationHelper;
import org.hibernate.boot.mapping.internal.xml.XmlProcessingHelper;
import org.hibernate.boot.mapping.internal.xml.db.TableProcessing;
import org.hibernate.boot.mapping.internal.xml.XmlDocumentContext;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;

import static org.hibernate.boot.mapping.internal.xml.attr.AnyMappingAttributeProcessing.applyDiscriminator;
import static org.hibernate.boot.mapping.internal.xml.attr.AnyMappingAttributeProcessing.applyKey;
import static org.hibernate.boot.mapping.internal.xml.attr.CommonAttributeProcessing.applyAccess;
import static org.hibernate.boot.mapping.internal.xml.attr.CommonAttributeProcessing.applyAttributeAccessor;
import static org.hibernate.boot.mapping.internal.xml.attr.CommonAttributeProcessing.applyFetching;
import static org.hibernate.boot.mapping.internal.xml.attr.CommonAttributeProcessing.applyOptimisticLock;
import static org.hibernate.boot.mapping.internal.xml.attr.CommonPluralAttributeProcessing.applyPluralAttributeStructure;
import static org.hibernate.internal.util.NullnessHelper.coalesce;

/**
 * @author Steve Ebersole
 */
public class PluralAnyMappingAttributeProcessing {

	@SuppressWarnings("UnusedReturnValue")
	public static MutableMemberDetails processPluralAnyMappingAttributes(
			JaxbPluralAnyMappingImpl jaxbHbmManyToAny,
			MutableClassDetails declarer,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		final AccessType accessType = coalesce( jaxbHbmManyToAny.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbHbmManyToAny.getName(),
				accessType,
				declarer
		);

		final ManyToAnyAnnotation manyToAnyAnn = (ManyToAnyAnnotation) memberDetails.applyAnnotationUsage(
				HibernateAnnotations.MANY_TO_ANY,
				xmlDocumentContext.getModelBuildingContext()
		);

		applyAccess( jaxbHbmManyToAny.getAccess(), memberDetails, xmlDocumentContext );
		applyAttributeAccessor( jaxbHbmManyToAny, memberDetails, xmlDocumentContext );
		applyFetching( jaxbHbmManyToAny, memberDetails, manyToAnyAnn, xmlDocumentContext );
		applyOptimisticLock( jaxbHbmManyToAny, memberDetails, xmlDocumentContext );

		applyDiscriminator( memberDetails, jaxbHbmManyToAny, xmlDocumentContext );
		applyKey( memberDetails, jaxbHbmManyToAny, xmlDocumentContext );

		XmlAnnotationHelper.applyCascading( jaxbHbmManyToAny.getCascade(), manyToAnyAnn, xmlDocumentContext );

		applyPluralAttributeStructure( jaxbHbmManyToAny, memberDetails, xmlDocumentContext );
		TableProcessing.transformJoinTable( jaxbHbmManyToAny.getJoinTable(), memberDetails, xmlDocumentContext );

		return memberDetails;
	}
}
