/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.HibernateException;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Transient;

import static org.hibernate.bytecode.enhance.internal.bytebuddy.model.ModelSourceLogging.MODEL_SOURCE_LOGGER;

/**
 * @author Steve Ebersole
 */
public class ModelSourceHelper {
	public static List<PersistentAttribute> buildPersistentAttributeList(
			ClassDetails managedTypeDetails,
			AccessType contextAccessType,
			ModelProcessingContext processingContext) {
		final AccessType classLevelAccessType = determineClassLevelAccessType(
				managedTypeDetails,
				managedTypeDetails.getIdentifierMember(),
				contextAccessType
		);
		if ( classLevelAccessType == null ) {
			throw new HibernateException( "Unable to determine class-level attribute access-type : " + managedTypeDetails.getName() );
		}

		MODEL_SOURCE_LOGGER.debugf( "Building PersistentAttribute list for %s using %s class-level access", managedTypeDetails.getName(), classLevelAccessType );

		final LinkedHashMap<String,MemberDetails> attributeMembers = collectBackingMembers( managedTypeDetails, classLevelAccessType );

		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	public static AccessType determineClassLevelAccessType(
			ClassDetails managedTypeDetails,
			MemberDetails identifierMember,
			AccessType contextAccessType) {
		final Access annotation = managedTypeDetails.getAnnotation( Access.class );
		if ( annotation != null ) {
			return annotation.value();
		}

		if ( managedTypeDetails.getSuperType() != null ) {
			final AccessType accessType = determineClassLevelAccessType(
					managedTypeDetails.getSuperType(),
					managedTypeDetails.getIdentifierMember(),
					null
			);
			if ( accessType != null ) {
				return accessType;
			}
		}

		if ( identifierMember != null ) {
			return identifierMember.getKind() == AnnotationTarget.Kind.FIELD
					? AccessType.FIELD
					: AccessType.PROPERTY;
		}

		return contextAccessType;
	}

	public static LinkedHashMap<String,MemberDetails> collectBackingMembers(
			ClassDetails managedTypeDetails,
			AccessType classLevelAccessType) {
		assert classLevelAccessType != null;

		final LinkedHashMap<String,MemberDetails> attributeMembers = new LinkedHashMap<>();
		final Consumer<MemberDetails> backingMemberConsumer = (memberDetails) -> {
			final MemberDetails previous = attributeMembers.put(
					memberDetails.resolveAttributeName(),
					memberDetails
			);
			if ( previous != null && previous != memberDetails) {
				throw new HibernateException( "Multiple backing members found : " + memberDetails.resolveAttributeName() );
			}
		};

		collectAttributeLevelAccessMembers( managedTypeDetails, backingMemberConsumer );
		collectClassLevelAccessMembers( classLevelAccessType, managedTypeDetails, backingMemberConsumer );

		return attributeMembers;
	}

	private static void collectAttributeLevelAccessMembers(
			ClassDetails managedTypeDetails,
			Consumer<MemberDetails> backingMemberConsumer) {
		for ( FieldDetails fieldDetails : managedTypeDetails.getFields() ) {
			if ( fieldDetails.hasAnnotation( Transient.class ) ) {
				continue;
			}

			final Access localAccess = fieldDetails.getAnnotation( Access.class );
			if ( localAccess == null ) {
				continue;
			}

			validateAttributeLevelAccess( fieldDetails, localAccess.value(), managedTypeDetails );

			backingMemberConsumer.accept( fieldDetails );
		}

		for ( MethodDetails methodDetails : managedTypeDetails.getMethods() ) {
			if ( methodDetails.getMethodKind() != MethodDetails.MethodKind.GETTER ) {
				continue;
			}

			if ( methodDetails.hasAnnotation( Transient.class ) ) {
				continue;
			}

			final Access localAccess = methodDetails.getAnnotation( Access.class );
			if ( localAccess == null ) {
				continue;
			}

			validateAttributeLevelAccess( methodDetails, localAccess.value(), managedTypeDetails );
			backingMemberConsumer.accept( methodDetails );
		}
	}

	private static void validateAttributeLevelAccess(
			MemberDetails annotationTarget,
			AccessType attributeAccessType,
			ClassDetails classDetails) {
		// Apply the checks defined in section `2.3.2 Explicit Access Type` of the persistence specification

		// Mainly, it is never legal to:
		//		1. specify @Access(FIELD) on a getter
		//		2. specify @Access(PROPERTY) on a field

		if ( ( attributeAccessType == AccessType.FIELD && annotationTarget.getKind() != AnnotationTarget.Kind.FIELD )
				|| ( attributeAccessType == AccessType.PROPERTY && annotationTarget.getKind() != AnnotationTarget.Kind.METHOD ) ) {
			throw new AccessTypePlacementException( classDetails, annotationTarget );
		}
	}

	private static void collectClassLevelAccessMembers(
			AccessType classLevelAccessType,
			ClassDetails managedTypeDetails,
			Consumer<MemberDetails> backingMemberConsumer) {
		if ( classLevelAccessType == AccessType.FIELD ) {
			processClassLevelAccessFields( managedTypeDetails, backingMemberConsumer );
		}
		else {
			processClassLevelAccessMethods( managedTypeDetails, backingMemberConsumer );
		}
	}

	private static void processClassLevelAccessFields(
			ClassDetails managedTypeDetails,
			Consumer<MemberDetails> backingMemberConsumer) {
		for ( FieldDetails fieldDetails : managedTypeDetails.getFields() ) {
			if ( fieldDetails.hasAnnotation( Transient.class ) ) {
				continue;
			}

			if ( fieldDetails.hasAnnotation( Access.class ) ) {
				// it would have been handled in #collectAttributeLevelAccessMembers
				continue;
			}

			backingMemberConsumer.accept( fieldDetails );
		}
	}

	private static void processClassLevelAccessMethods(
			ClassDetails managedTypeDetails,
			Consumer<MemberDetails> backingMemberConsumer) {
		for ( MethodDetails methodDetails : managedTypeDetails.getMethods() ) {
			if ( methodDetails.getMethodKind() != MethodDetails.MethodKind.GETTER ) {
				continue;
			}

			if ( methodDetails.hasAnnotation( Transient.class ) ) {
				continue;
			}

			if ( methodDetails.hasAnnotation( Access.class ) ) {
				// it would have been handled in #collectAttributeLevelAccessMembers
				continue;
			}

			backingMemberConsumer.accept( methodDetails );
		}

	}
}
