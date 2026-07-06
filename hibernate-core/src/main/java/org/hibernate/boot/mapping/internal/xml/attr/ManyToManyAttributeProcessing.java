/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.xml.attr;

import jakarta.persistence.AccessType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToManyImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.XmlAnnotations;
import org.hibernate.boot.models.annotations.internal.ManyToManyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TargetXmlAnnotation;
import org.hibernate.boot.mapping.internal.xml.XmlAnnotationHelper;
import org.hibernate.boot.mapping.internal.xml.XmlProcessingHelper;
import org.hibernate.boot.mapping.internal.xml.db.TableProcessing;
import org.hibernate.boot.mapping.internal.xml.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;

import static org.hibernate.boot.mapping.internal.xml.attr.CommonAttributeProcessing.applyAccess;
import static org.hibernate.boot.mapping.internal.xml.attr.CommonAttributeProcessing.applyAttributeAccessor;
import static org.hibernate.boot.mapping.internal.xml.attr.CommonAttributeProcessing.applyFetching;
import static org.hibernate.boot.mapping.internal.xml.attr.CommonAttributeProcessing.applyOptimisticLock;
import static org.hibernate.boot.mapping.internal.xml.attr.CommonPluralAttributeProcessing.applyPluralAttributeStructure;
import static org.hibernate.internal.util.NullnessHelper.coalesce;

/**
 * @author Steve Ebersole
 */
public class ManyToManyAttributeProcessing {

	@SuppressWarnings("UnusedReturnValue")
	public static MutableMemberDetails processManyToManyAttribute(
			JaxbManyToManyImpl jaxbManyToMany,
			MutableClassDetails declarer,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		final AccessType accessType = coalesce( jaxbManyToMany.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbManyToMany.getName(),
				accessType,
				declarer
		);

		final ManyToManyJpaAnnotation manyToManyAnn = applyManyToMany(
				jaxbManyToMany,
				memberDetails,
				xmlDocumentContext
		);

		applyTarget( jaxbManyToMany, xmlDocumentContext, memberDetails );

		XmlAnnotationHelper.applyCascading( jaxbManyToMany.getCascade(), manyToManyAnn, xmlDocumentContext );

		applyAccess( jaxbManyToMany.getAccess(), memberDetails, xmlDocumentContext );
		applyAttributeAccessor( jaxbManyToMany, memberDetails, xmlDocumentContext );
		applyFetching( jaxbManyToMany, memberDetails, manyToManyAnn, xmlDocumentContext );

		applyPluralAttributeStructure( jaxbManyToMany, memberDetails, xmlDocumentContext );

		XmlAnnotationHelper.applyAttributeOverrides(
				jaxbManyToMany.getMapKeyAttributeOverrides(),
				memberDetails,
				"key",
				xmlDocumentContext
		);

		TableProcessing.transformJoinTable( jaxbManyToMany.getJoinTable(), memberDetails, xmlDocumentContext );

		XmlAnnotationHelper.applySqlJoinTableRestriction( jaxbManyToMany.getSqlJoinTableRestriction(), memberDetails,
				xmlDocumentContext );

		XmlAnnotationHelper.applyJoinTableFilters( jaxbManyToMany.getJoinTableFilters(), memberDetails,
				xmlDocumentContext );

		XmlAnnotationHelper.applyNotFound( jaxbManyToMany, memberDetails, xmlDocumentContext );

		applyOptimisticLock( jaxbManyToMany, memberDetails, xmlDocumentContext );

		return memberDetails;
	}

	private static void applyTarget(
			JaxbManyToManyImpl jaxbManyToMany,
			XmlDocumentContext xmlDocumentContext,
			MutableMemberDetails memberDetails) {
		if ( StringHelper.isEmpty( jaxbManyToMany.getTargetEntity() ) ) {
			return;
		}
		final TargetXmlAnnotation targetAnn = (TargetXmlAnnotation) memberDetails.applyAnnotationUsage(
				XmlAnnotations.TARGET,
				xmlDocumentContext.getModelsContext()
		);
		targetAnn.value( xmlDocumentContext.resolveTargetEntityName( jaxbManyToMany.getTargetEntity() ) );
	}

	private static ManyToManyJpaAnnotation applyManyToMany(
			JaxbManyToManyImpl jaxbManyToMany,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final ManyToManyJpaAnnotation manyToManyAnn = (ManyToManyJpaAnnotation) memberDetails.applyAnnotationUsage(
				JpaAnnotations.MANY_TO_MANY,
				xmlDocumentContext.getModelsContext()
		);

		CommonAttributeProcessing.applyFetching( jaxbManyToMany, memberDetails, manyToManyAnn, xmlDocumentContext );
		CommonAttributeProcessing.applyMappedBy( jaxbManyToMany.getMappedBy(), manyToManyAnn, xmlDocumentContext );

		return manyToManyAnn;
	}

}
