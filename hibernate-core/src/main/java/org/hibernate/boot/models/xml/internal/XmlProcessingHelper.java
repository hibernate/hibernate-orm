/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal;

import java.beans.Introspector;
import java.lang.annotation.Annotation;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManagedType;
import org.hibernate.boot.models.MemberResolutionException;
import org.hibernate.boot.models.internal.AnnotationUsageHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.MutableAnnotationUsage;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.AccessType;

/**
 * Common helper utilities for handling mapping XML processing
 *
 * @author Steve Ebersole
 */
public class XmlProcessingHelper {
	/**
	 * Determine the name of a class defined in XML, accounting for {@code <package/>}
	 *
	 * @param jaxbRoot The {@code <entity-mappings/>} node for access to the package (if one)
	 * @param jaxbManagedType The class JAXB node
	 */
	public static String determineClassName(JaxbEntityMappingsImpl jaxbRoot, JaxbManagedType jaxbManagedType) {
		return StringHelper.qualifyConditionallyIfNot( jaxbRoot.getPackage(), jaxbManagedType.getClazz() );
	}

	public static AccessType inverse(AccessType accessType) {
		return accessType == AccessType.FIELD ? AccessType.PROPERTY : AccessType.FIELD;
	}

	/**
	 * Find the member backing the named attribute
	 */
	public static MutableMemberDetails getAttributeMember(
			String attributeName,
			AccessType accessType,
			MutableClassDetails classDetails) {
		final MutableMemberDetails result = findAttributeMember(
				attributeName,
				accessType,
				classDetails
		);
		if ( result == null ) {
			throw new MemberResolutionException(
					String.format(
							"Could not locate attribute member - %s (%s)",
							attributeName,
							classDetails.getName()
					)
			);
		}
		return result;
	}

	/**
	 * Find the member backing the named attribute
	 */
	public static MutableMemberDetails findAttributeMember(
			String attributeName,
			AccessType accessType,
			MutableClassDetails classDetails) {
		if ( accessType == AccessType.PROPERTY ) {
			for ( int i = 0; i < classDetails.getMethods().size(); i++ ) {
				final MethodDetails methodDetails = classDetails.getMethods().get( i );
				if ( methodDetails.getMethodKind() == MethodDetails.MethodKind.GETTER ) {
					if ( methodDetails.getName().startsWith( "get" ) ) {
						final String stemName = methodDetails.getName().substring( 3 );
						final String decapitalizedStemName = Introspector.decapitalize( stemName );
						if ( stemName.equals( attributeName ) || decapitalizedStemName.equals( attributeName ) ) {
							return (MutableMemberDetails) methodDetails;
						}
					}
					else if ( methodDetails.getName().startsWith( "is" ) ) {
						final String stemName = methodDetails.getName().substring( 2 );
						final String decapitalizedStemName = Introspector.decapitalize( stemName );
						if ( stemName.equals( attributeName ) || decapitalizedStemName.equals( attributeName ) ) {
							return (MutableMemberDetails) methodDetails;
						}
					}
				}
			}
		}
		else if ( accessType == AccessType.FIELD ) {
			for ( int i = 0; i < classDetails.getFields().size(); i++ ) {
				final FieldDetails fieldDetails = classDetails.getFields().get( i );
				if ( fieldDetails.getName().equals( attributeName ) ) {
					return (MutableMemberDetails) fieldDetails;
				}
			}
		}

		return null;
	}


	public static <A extends Annotation> void applyAttributeIfSpecified(
			String attributeName,
			String value,
			MutableAnnotationUsage<A> annotationUsage) {
		AnnotationUsageHelper.applyStringAttributeIfSpecified( attributeName, value, annotationUsage );
	}

	public static <A extends Annotation> void applyAttributeIfSpecified(
			String attributeName,
			Object value,
			MutableAnnotationUsage<A> annotationUsage) {
		AnnotationUsageHelper.applyAttributeIfSpecified( attributeName, value, annotationUsage );
	}
}
