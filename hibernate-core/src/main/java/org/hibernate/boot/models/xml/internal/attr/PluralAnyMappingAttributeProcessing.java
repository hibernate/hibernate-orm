/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal.attr;

import jakarta.persistence.AccessType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralAnyMappingImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.internal.ManyToAnyAnnotation;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.internal.db.TableProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;

import static org.hibernate.boot.models.xml.internal.attr.AnyMappingAttributeProcessing.applyDiscriminator;
import static org.hibernate.boot.models.xml.internal.attr.AnyMappingAttributeProcessing.applyKey;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyAccess;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyAttributeAccessor;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyFetching;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyOptimisticLock;
import static org.hibernate.boot.models.xml.internal.attr.CommonPluralAttributeProcessing.applyPluralAttributeStructure;
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

		applyAccess( accessType, memberDetails, xmlDocumentContext );
		applyAttributeAccessor( jaxbHbmManyToAny, memberDetails, xmlDocumentContext );
		applyFetching( jaxbHbmManyToAny, memberDetails, manyToAnyAnn, xmlDocumentContext );
		applyOptimisticLock( jaxbHbmManyToAny, memberDetails, xmlDocumentContext );

		applyDiscriminator( memberDetails, jaxbHbmManyToAny, xmlDocumentContext );
		applyKey( memberDetails, jaxbHbmManyToAny, xmlDocumentContext );

		XmlAnnotationHelper.applyCascading( jaxbHbmManyToAny.getCascade(), memberDetails, xmlDocumentContext );

		applyPluralAttributeStructure( jaxbHbmManyToAny, memberDetails, xmlDocumentContext );
		TableProcessing.transformJoinTable( jaxbHbmManyToAny.getJoinTable(), memberDetails, xmlDocumentContext );

		return memberDetails;
	}
}
