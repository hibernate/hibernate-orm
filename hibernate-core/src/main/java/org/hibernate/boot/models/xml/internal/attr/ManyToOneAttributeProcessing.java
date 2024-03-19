/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal.attr;

import java.util.EnumSet;
import java.util.List;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.internal.Target;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToOneImpl;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.MutableAnnotationUsage;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.AccessType;
import jakarta.persistence.ManyToOne;

import static org.hibernate.boot.models.xml.internal.XmlAnnotationHelper.determineTargetName;
import static org.hibernate.internal.util.NullnessHelper.coalesce;

/**
 * @author Steve Ebersole
 */
public class ManyToOneAttributeProcessing {

	public static MutableMemberDetails processManyToOneAttribute(
			JaxbManyToOneImpl jaxbManyToOne,
			MutableClassDetails declarer,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		final AccessType accessType = coalesce( jaxbManyToOne.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbManyToOne.getName(),
				accessType,
				declarer
		);

		final MutableAnnotationUsage<ManyToOne> manyToOneAnn = applyManyToOne(
				memberDetails,
				jaxbManyToOne,
				xmlDocumentContext
		);

		CommonAttributeProcessing.applyAttributeBasics( jaxbManyToOne, memberDetails, manyToOneAnn, accessType, xmlDocumentContext );

		XmlAnnotationHelper.applyJoinColumns( jaxbManyToOne.getJoinColumns(), memberDetails, xmlDocumentContext );
		applyNotFound( memberDetails, jaxbManyToOne, manyToOneAnn, xmlDocumentContext );
		applyOnDelete( memberDetails, jaxbManyToOne, manyToOneAnn, xmlDocumentContext );
		applyTarget( memberDetails, jaxbManyToOne, manyToOneAnn, xmlDocumentContext );
		XmlAnnotationHelper.applyCascading( jaxbManyToOne.getCascade(), memberDetails, xmlDocumentContext );

		return memberDetails;
	}

	private static MutableAnnotationUsage<ManyToOne> applyManyToOne(
			MutableMemberDetails memberDetails,
			JaxbManyToOneImpl jaxbManyToOne,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<ManyToOne> manyToOneAnn = XmlProcessingHelper.getOrMakeAnnotation( ManyToOne.class, memberDetails, xmlDocumentContext );
		final AnnotationDescriptor<ManyToOne> manyToOneDescriptor = xmlDocumentContext
				.getModelBuildingContext()
				.getAnnotationDescriptorRegistry()
				.getDescriptor( ManyToOne.class );

		XmlAnnotationHelper.applyOr(
				jaxbManyToOne,
				JaxbManyToOneImpl::getFetch,
				"fetch",
				manyToOneAnn,
				manyToOneDescriptor
		);

		return manyToOneAnn;
	}

	private static void applyNotFound(
			MutableMemberDetails memberDetails,
			JaxbManyToOneImpl jaxbManyToOne,
			MutableAnnotationUsage<ManyToOne> manyToOneAnn,
			XmlDocumentContext xmlDocumentContext) {
		final NotFoundAction notFoundAction = jaxbManyToOne.getNotFound();
		if ( notFoundAction == null ) {
			return;
		}

		final MutableAnnotationUsage<NotFound> notFoundAnn = XmlProcessingHelper.makeAnnotation( NotFound.class, memberDetails, xmlDocumentContext );
		notFoundAnn.setAttributeValue( "action", notFoundAction );
	}

	@SuppressWarnings("unused")
	private static void applyOnDelete(
			MutableMemberDetails memberDetails,
			JaxbManyToOneImpl jaxbManyToOne,
			MutableAnnotationUsage<ManyToOne> manyToOneAnn,
			XmlDocumentContext xmlDocumentContext) {
		final OnDeleteAction action = jaxbManyToOne.getOnDelete();
		if ( action == null ) {
			return;
		}

		final MutableAnnotationUsage<OnDelete> notFoundAnn = XmlProcessingHelper.makeAnnotation( OnDelete.class, memberDetails, xmlDocumentContext );
		notFoundAnn.setAttributeValue( "action", action );
	}

	@SuppressWarnings("unused")
	private static void applyTarget(
			MutableMemberDetails memberDetails,
			JaxbManyToOneImpl jaxbManyToOne,
			MutableAnnotationUsage<ManyToOne> manyToOneAnn,
			XmlDocumentContext xmlDocumentContext) {
		final String targetEntityName = jaxbManyToOne.getTargetEntity();
		if ( StringHelper.isEmpty( targetEntityName ) ) {
			return;
		}

		final MutableAnnotationUsage<Target> targetAnn = XmlProcessingHelper.makeAnnotation( Target.class, memberDetails, xmlDocumentContext );
		targetAnn.setAttributeValue( "value", determineTargetName( targetEntityName, xmlDocumentContext ) );
	}

	private static <E extends Enum<E>> List<E> asList(EnumSet<E> enums) {
		final List<E> list = CollectionHelper.arrayList( enums.size() );
		list.addAll( enums );
		return list;
	}
}
