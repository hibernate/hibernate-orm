/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal.attr;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.AttributeAccessor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLockableAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistentAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSingularAssociationAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSingularFetchModeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbStandardAttribute;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.models.internal.MutableAnnotationUsage;
import org.hibernate.models.internal.MutableMemberDetails;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.FetchType;

/**
 * @author Steve Ebersole
 */
public class CommonAttributeProcessing {

	public static <A extends Annotation> void applyAttributeBasics(
			JaxbPersistentAttribute jaxbAttribute,
			MutableMemberDetails memberDetails,
			MutableAnnotationUsage<A> attributeAnn,
			AccessType accessType,
			XmlDocumentContext xmlDocumentContext) {
		applyAccess( accessType, memberDetails, xmlDocumentContext );
		applyAttributeAccessor( jaxbAttribute, memberDetails, xmlDocumentContext );

		if ( jaxbAttribute instanceof JaxbStandardAttribute jaxbStandardAttribute ) {
			applyFetching( jaxbStandardAttribute, memberDetails, attributeAnn, xmlDocumentContext );
			applyOptionality( jaxbStandardAttribute, memberDetails, attributeAnn, xmlDocumentContext );
		}

		if ( jaxbAttribute instanceof JaxbLockableAttribute jaxbLockableAttribute ) {
			applyOptimisticLock( jaxbLockableAttribute, memberDetails, xmlDocumentContext );
		}
	}

	public static void applyAccess(
			AccessType accessType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<Access> accessAnn = XmlProcessingHelper.makeAnnotation( Access.class, memberDetails, xmlDocumentContext );
		accessAnn.setAttributeValue( "value", accessType );
	}

	public static <A extends Annotation> void applyAttributeAccessor(
			JaxbPersistentAttribute jaxbAttribute,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final String attributeAccessor = jaxbAttribute.getAttributeAccessor();
		if ( attributeAccessor == null ) {
			return;
		}

		final ClassDetails strategyClassDetails = xmlDocumentContext
				.getModelBuildingContext()
				.getClassDetailsRegistry()
				.getClassDetails( attributeAccessor );
		final MutableAnnotationUsage<AttributeAccessor> accessAnn = XmlProcessingHelper.makeAnnotation( AttributeAccessor.class, memberDetails, xmlDocumentContext );
		accessAnn.setAttributeValue( "strategy", strategyClassDetails );
	}

	public static <A extends Annotation> void applyOptionality(
			JaxbStandardAttribute jaxbAttribute,
			MutableMemberDetails memberDetails,
			MutableAnnotationUsage<A> attributeAnn,
			XmlDocumentContext xmlDocumentContext) {
		// todo : fix this in jpa32
		if ( jaxbAttribute.isOptional() == null ) {
			return;
		}

		attributeAnn.setAttributeValue( "optional", jaxbAttribute.isOptional() );
	}

	public static <A extends Annotation> void applyOptimisticLock(
			JaxbLockableAttribute jaxbAttribute,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final boolean includeInOptimisticLock = jaxbAttribute.isOptimisticLock();
		final MutableAnnotationUsage<OptimisticLock> optLockAnn = XmlProcessingHelper.makeAnnotation( OptimisticLock.class, memberDetails, xmlDocumentContext );
		optLockAnn.setAttributeValue( "excluded", !includeInOptimisticLock );
	}

	public static <A extends Annotation> void applyFetching(
			JaxbStandardAttribute jaxbAttribute,
			MutableMemberDetails memberDetails,
			MutableAnnotationUsage<A> attributeAnn,
			XmlDocumentContext xmlDocumentContext) {
		final FetchType fetchType = jaxbAttribute.getFetch();
		if ( fetchType != null ) {
			attributeAnn.setAttributeValue( "fetch", fetchType );
		}

		if ( jaxbAttribute instanceof JaxbSingularAssociationAttribute jaxbSingularAttribute ) {
			final JaxbSingularFetchModeImpl jaxbFetchMode = jaxbSingularAttribute.getFetchMode();
			applyFetchMode( memberDetails, jaxbFetchMode, xmlDocumentContext );
		}
		else if ( jaxbAttribute instanceof JaxbAnyMappingImpl jaxbAnyAttribute ) {
			final JaxbSingularFetchModeImpl jaxbFetchMode = jaxbAnyAttribute.getFetchMode();
			applyFetchMode( memberDetails, jaxbFetchMode, xmlDocumentContext );
		}
	}

	private static void applyFetchMode(
			MutableMemberDetails memberDetails,
			JaxbSingularFetchModeImpl jaxbFetchMode,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbFetchMode != null ) {
			final FetchMode fetchMode = FetchMode.valueOf( jaxbFetchMode.value() );
			final MutableAnnotationUsage<Fetch> fetchAnn = XmlProcessingHelper.makeAnnotation( Fetch.class, memberDetails, xmlDocumentContext );
			fetchAnn.setAttributeValue( "value", fetchMode );
		}
	}
}
