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
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.internal.dynamic.DynamicAnnotationUsage;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.MutableAnnotationTarget;
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

	/**
	 * Find an existing annotation, or create one.
	 * Used when applying XML in override mode.
	 */
	public static <A extends Annotation> MutableAnnotationUsage<A> getOrMakeAnnotation(
			Class<A> annotationType,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		final AnnotationUsage<A> existing = target.getAnnotationUsage( annotationType );
		if ( existing != null ) {
			return (MutableAnnotationUsage<A>) existing;
		}

		return makeAnnotation( annotationType, target, xmlDocumentContext );
	}

	public static <A extends Annotation> MutableAnnotationUsage<A> getOrMakeAnnotation(
			Class<A> annotationType,
			XmlDocumentContext xmlDocumentContext) {

		return makeAnnotation( annotationType,  xmlDocumentContext );
	}

	/**
	 * Make a nested AnnotationUsage.  The usage is created with the given target,
	 * but it is not added to the target's annotations.
	 */
	public static <A extends Annotation> MutableAnnotationUsage<A> makeNestedAnnotation(
			Class<A> annotationType,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		return new DynamicAnnotationUsage<>(
				xmlDocumentContext.getModelBuildingContext()
						.getAnnotationDescriptorRegistry()
						.getDescriptor( annotationType ),
				target
		);
	}

	public static <A extends Annotation> MutableAnnotationUsage<A> makeNestedAnnotation(
			Class<A> annotationType,
			XmlDocumentContext xmlDocumentContext) {
		return new DynamicAnnotationUsage<>(
				xmlDocumentContext.getModelBuildingContext()
						.getAnnotationDescriptorRegistry()
						.getDescriptor( annotationType )
		);
	}

	/**
	 * Make an AnnotationUsage.
	 * Used when applying XML in complete mode or when {@linkplain #getOrMakeAnnotation}
	 * needs to make.
	 */
	public static <A extends Annotation> MutableAnnotationUsage<A> makeAnnotation(
			Class<A> annotationType,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<A> created = makeNestedAnnotation( annotationType, target, xmlDocumentContext );
		target.addAnnotationUsage( created );
		return created;
	}

	public static <A extends Annotation> MutableAnnotationUsage<A> makeAnnotation(
			Class<A> annotationType,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<A> created = makeNestedAnnotation( annotationType, xmlDocumentContext );
		return created;
	}

	/**
	 * Find an existing annotation by name, or create one.
	 * Used when applying XML in override mode.
	 */
	public static <A extends Annotation> MutableAnnotationUsage<A> getOrMakeNamedAnnotation(
			Class<A> annotationType,
			String name,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		return getOrMakeNamedAnnotation( annotationType, name, "name", target, xmlDocumentContext );
	}

	/**
	 * Find an existing annotation by name, or create one.
	 * Used when applying XML in override mode.
	 */
	public static <A extends Annotation> MutableAnnotationUsage<A> getOrMakeNamedAnnotation(
			Class<A> annotationType,
			String name,
			String attributeToMatch,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		if ( name == null ) {
			return makeAnnotation( annotationType, target, xmlDocumentContext );
		}

		final AnnotationUsage<A> existing = target.getNamedAnnotationUsage( annotationType, name, attributeToMatch );
		if ( existing != null ) {
			return (MutableAnnotationUsage<A>) existing;
		}

		return makeNamedAnnotation( annotationType, name, attributeToMatch, target, xmlDocumentContext );
	}

	/**
	 * Make a named AnnotationUsage.
	 * Used when applying XML in complete mode or when {@linkplain #getOrMakeNamedAnnotation}
	 * needs to make.
	 */
	public static <A extends Annotation> MutableAnnotationUsage<A> makeNamedAnnotation(
			Class<A> annotationType,
			String name,
			String nameAttributeName,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<A> created = makeNestedAnnotation( annotationType, target, xmlDocumentContext );
		target.addAnnotationUsage( created );
		created.setAttributeValue( nameAttributeName, name );
		return created;
	}

	public static <A extends Annotation> void setIf(
			Object value,
			String attributeName,
			MutableAnnotationUsage<A> annotationUsage) {
		if ( value == null ) {
			return;
		}

		if ( value instanceof String && ( (String) value ).isBlank() ) {
			return;
		}

		annotationUsage.setAttributeValue( attributeName, value );
	}
}
