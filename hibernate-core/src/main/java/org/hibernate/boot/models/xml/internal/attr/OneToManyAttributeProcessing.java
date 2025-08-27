/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal.attr;

import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToManyImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.XmlAnnotations;
import org.hibernate.boot.models.annotations.internal.OneToManyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TargetXmlAnnotation;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.internal.db.JoinColumnProcessing;
import org.hibernate.boot.models.xml.internal.db.TableProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.MutableAnnotationTarget;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.AccessType;

import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyAccess;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyAttributeAccessor;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyFetching;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyOptimisticLock;
import static org.hibernate.internal.util.NullnessHelper.coalesce;

/**
 * @author Steve Ebersole
 */
public class OneToManyAttributeProcessing {
	public static MutableMemberDetails processOneToManyAttribute(
			JaxbOneToManyImpl jaxbOneToMany,
			MutableClassDetails declarer,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		final AccessType accessType = coalesce( jaxbOneToMany.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbOneToMany.getName(),
				accessType,
				declarer
		);

		final OneToManyJpaAnnotation oneToManyAnn = applyOneToMany(
				jaxbOneToMany,
				memberDetails,
				accessType,
				xmlDocumentContext
		);

		applyTargetEntity( jaxbOneToMany, oneToManyAnn, memberDetails, xmlDocumentContext );

		CommonPluralAttributeProcessing.applyPluralAttributeStructure( jaxbOneToMany, memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyCascading( jaxbOneToMany.getCascade(), memberDetails, xmlDocumentContext );

		XmlAnnotationHelper.applyAttributeOverrides(
				jaxbOneToMany.getMapKeyAttributeOverrides(),
				memberDetails,
				"key",
				xmlDocumentContext
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// join-table

		TableProcessing.transformJoinTable( jaxbOneToMany.getJoinTable(), memberDetails, xmlDocumentContext );

		XmlAnnotationHelper.applySqlJoinTableRestriction( jaxbOneToMany.getSqlJoinTableRestriction(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyJoinTableFilters( jaxbOneToMany.getJoinTableFilters(), memberDetails, xmlDocumentContext );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// other properties

		JoinColumnProcessing.applyJoinColumns( jaxbOneToMany.getJoinColumn(), memberDetails, xmlDocumentContext );

		CommonAttributeProcessing.applyOnDelete( jaxbOneToMany.getOnDelete(), memberDetails, xmlDocumentContext );

		XmlAnnotationHelper.applyNotFound( jaxbOneToMany, memberDetails, xmlDocumentContext );

		return memberDetails;
	}

	private static OneToManyJpaAnnotation applyOneToMany(
			JaxbOneToManyImpl jaxbOneToMany,
			MutableMemberDetails memberDetails,
			AccessType accessType,
			XmlDocumentContext xmlDocumentContext) {
		final OneToManyJpaAnnotation oneToManyAnn = (OneToManyJpaAnnotation) memberDetails.applyAnnotationUsage(
				JpaAnnotations.ONE_TO_MANY,
				xmlDocumentContext.getModelBuildingContext()
		);

		if ( jaxbOneToMany != null ) {
			applyAccess( accessType, memberDetails, xmlDocumentContext );
			applyAttributeAccessor( jaxbOneToMany, memberDetails, xmlDocumentContext );
			applyFetching( jaxbOneToMany, memberDetails, oneToManyAnn, xmlDocumentContext );
			applyOptimisticLock( jaxbOneToMany, memberDetails, xmlDocumentContext );

			if ( StringHelper.isNotEmpty( jaxbOneToMany.getMappedBy() ) ) {
				oneToManyAnn.mappedBy( jaxbOneToMany.getMappedBy() );
			}

			if ( jaxbOneToMany.isOrphanRemoval() != null ) {
				oneToManyAnn.orphanRemoval( jaxbOneToMany.isOrphanRemoval() );
			}
		}

		return oneToManyAnn;
	}

	private static void applyTargetEntity(
			JaxbOneToManyImpl jaxbOneToMany,
			OneToManyJpaAnnotation oneToManyAnn,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		// todo (7.0) : we need a distinction here between hbm.xml target and orm.xml target-entity
		//		- for orm.xml target-entity we should apply the package name, if one
		//		- for hbm.xml target we should not since it could refer to a dynamic mapping
		//
		// todo (7.0) : also, should we ever use `@ManyToOne#targetEntity`?
		//  	or just always use Hibernate's `@Target`?
		final String targetEntityName = jaxbOneToMany.getTargetEntity();
		if ( StringHelper.isEmpty( targetEntityName ) ) {
			return;
		}
		final TargetXmlAnnotation annotation = (TargetXmlAnnotation) target.applyAnnotationUsage(
				XmlAnnotations.TARGET,
				xmlDocumentContext.getModelBuildingContext()
		);
		annotation.value( xmlDocumentContext.resolveClassName( targetEntityName ) );
	}
}
