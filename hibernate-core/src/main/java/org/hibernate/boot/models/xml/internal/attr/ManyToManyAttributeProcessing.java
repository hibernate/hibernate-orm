/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal.attr;

import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToManyImpl;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.internal.db.TableProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.MutableAnnotationUsage;

import jakarta.persistence.AccessType;
import jakarta.persistence.ManyToMany;

import static org.hibernate.boot.models.xml.internal.XmlProcessingHelper.getOrMakeAnnotation;
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

		final MutableAnnotationUsage<ManyToMany> manyToManyAnn = applyManyToMany(
				jaxbManyToMany,
				memberDetails,
				xmlDocumentContext
		);

		applyTargetEntity( jaxbManyToMany, manyToManyAnn, xmlDocumentContext );

		XmlAnnotationHelper.applyCascading( jaxbManyToMany.getCascade(), memberDetails, xmlDocumentContext );

		CommonAttributeProcessing.applyAttributeBasics( jaxbManyToMany, memberDetails, manyToManyAnn, accessType, xmlDocumentContext );

		CommonPluralAttributeProcessing.applyPluralAttributeStructure( jaxbManyToMany, memberDetails, xmlDocumentContext );

		TableProcessing.applyJoinTable( jaxbManyToMany.getJoinTable(), memberDetails, xmlDocumentContext );

		XmlAnnotationHelper.applySqlJoinTableRestriction( jaxbManyToMany.getSqlJoinTableRestriction(), memberDetails, xmlDocumentContext );

		jaxbManyToMany.getJoinTableFilters().forEach( (jaxbFilter) -> {
			XmlAnnotationHelper.applyJoinTableFilter( jaxbFilter, memberDetails, xmlDocumentContext );
		} );

		return memberDetails;
	}

	private static MutableAnnotationUsage<ManyToMany> applyManyToMany(
			JaxbManyToManyImpl jaxbManyToMany,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<ManyToMany> manyToManyAnn = getOrMakeAnnotation(
				ManyToMany.class,
				memberDetails,
				xmlDocumentContext
		);
		final AnnotationDescriptor<ManyToMany> manyToManyDescriptor = xmlDocumentContext
				.getModelBuildingContext()
				.getAnnotationDescriptorRegistry()
				.getDescriptor( ManyToMany.class );

		XmlAnnotationHelper.applyOr( jaxbManyToMany, JaxbManyToManyImpl::getFetch, "fetch", manyToManyAnn, manyToManyDescriptor );
		XmlAnnotationHelper.applyOr( jaxbManyToMany, JaxbManyToManyImpl::getMappedBy, "mappedBy", manyToManyAnn, manyToManyDescriptor );

		return manyToManyAnn;
	}

	private static void applyTargetEntity(
			JaxbManyToManyImpl jaxbManyToMany,
			MutableAnnotationUsage<ManyToMany> manyToManyAnn,
			XmlDocumentContext xmlDocumentContext) {
		final String targetEntity = jaxbManyToMany.getTargetEntity();
		if ( StringHelper.isNotEmpty( targetEntity ) ) {
			manyToManyAnn.setAttributeValue(
					"targetEntity",
					xmlDocumentContext.getModelBuildingContext()
							.getClassDetailsRegistry()
							.resolveClassDetails( targetEntity )
			);
		}
	}
}
