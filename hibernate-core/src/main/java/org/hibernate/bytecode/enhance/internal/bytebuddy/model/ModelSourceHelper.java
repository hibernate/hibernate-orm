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
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.impl.PersistentAttributeFactory;
import org.hibernate.internal.util.StringHelper;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Transient;

import static org.hibernate.bytecode.enhance.internal.bytebuddy.model.ModelSourceLogging.MODEL_SOURCE_LOGGER;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * @author Steve Ebersole
 */
public class ModelSourceHelper {
	public static List<PersistentAttribute> buildPersistentAttributeList(
			ClassDetails declaringType,
			AccessType contextAccessType,
			ModelProcessingContext processingContext) {
		final AccessType classLevelAccessType = determineClassLevelAccessType(
				declaringType,
				declaringType.getIdentifierMember(),
				contextAccessType
		);
		if ( classLevelAccessType == null ) {
			throw new HibernateException( "Unable to determine class-level attribute access-type : " + declaringType.getName() );
		}

		MODEL_SOURCE_LOGGER.debugf( "Building PersistentAttribute list for %s using %s class-level access", declaringType.getName(), classLevelAccessType );

		final LinkedHashMap<String,MemberDetails> attributeMembers = collectBackingMembers(
				declaringType,
				classLevelAccessType
		);
		final List<PersistentAttribute> attributes = arrayList( attributeMembers.size() );
		attributeMembers.forEach( (attributeName, backingMemberDetails) -> {
			final PersistentAttribute attributeDescriptor = buildPersistentAttribute( attributeName, backingMemberDetails, declaringType, processingContext );
			attributes.add( attributeDescriptor );
		} );

		return attributes;
	}

	private static PersistentAttribute buildPersistentAttribute(
			String attributeName,
			MemberDetails backingMemberDetails,
			ClassDetails declaringType,
			ModelProcessingContext processingContext) {
		assert backingMemberDetails.getKind() == AnnotationTarget.Kind.FIELD
				|| backingMemberDetails.getKind() == AnnotationTarget.Kind.METHOD;

		if ( backingMemberDetails.getKind() == AnnotationTarget.Kind.FIELD ) {
			final PersistentAttributeFactory builder = new PersistentAttributeFactory(
					declaringType,
					attributeName,
					AccessType.FIELD,
					(FieldDetails) backingMemberDetails
			);

			final String methodNameStem = backingMemberDetails.resolveAttributeMethodNameStem();
			assert StringHelper.isNotEmpty( methodNameStem );

			for ( int i = 0; i < declaringType.getMethods().size(); i++ ) {
				final MethodDetails methodDetails = declaringType.getMethods().get( i );
				if ( methodDetails.getMethodKind() == MethodDetails.MethodKind.GETTER ) {
					if ( methodNameStem.equals( methodDetails.resolveAttributeMethodNameStem() ) ) {
						builder.setGetterMethod( methodDetails );
					}
				}
				else if ( methodDetails.getMethodKind() == MethodDetails.MethodKind.SETTER ) {
					if ( methodNameStem.equals( methodDetails.resolveAttributeMethodNameStem() ) ) {
						builder.setSetterMethod( methodDetails );
					}
				}
			}

			return builder.buildPersistentAttribute();
		}

		assert backingMemberDetails.getKind() == AnnotationTarget.Kind.METHOD;
		final PersistentAttributeFactory builder = new PersistentAttributeFactory(
				declaringType,
				attributeName,
				AccessType.PROPERTY,
				(MethodDetails) backingMemberDetails
		);

		for ( int i = 0; i < declaringType.getFields().size(); i++ ) {
			final FieldDetails fieldDetails = declaringType.getFields().get( i );
			if ( attributeName.equals( fieldDetails.getName() ) ) {
				builder.setBackingField( fieldDetails );
			}
		}

		for ( int i = 0; i < declaringType.getMethods().size(); i++ ) {
			final MethodDetails methodDetails = declaringType.getMethods().get( i );
			if ( methodDetails.getMethodKind() == MethodDetails.MethodKind.SETTER ) {
				if ( backingMemberDetails.resolveAttributeMethodNameStem().equals( methodDetails.resolveAttributeMethodNameStem() ) ) {
					builder.setSetterMethod( methodDetails );
				}
			}
		}

		return builder.buildPersistentAttribute();
	}

	public static AccessType determineClassLevelAccessType(
			ClassDetails declaringType,
			MemberDetails identifierMember,
			AccessType contextAccessType) {
		final Access annotation = declaringType.getAnnotation( Access.class );
		if ( annotation != null ) {
			return annotation.value();
		}

		if ( declaringType.getSuperType() != null ) {
			final AccessType accessType = determineClassLevelAccessType(
					declaringType.getSuperType(),
					declaringType.getIdentifierMember(),
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
			ClassDetails declaringType,
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

		collectAttributeLevelAccessMembers( declaringType, backingMemberConsumer );
		collectClassLevelAccessMembers( classLevelAccessType, declaringType, backingMemberConsumer );

		return attributeMembers;
	}

	/**
	 * Perform an action for each member which locally define an `AccessType` via `@Access`
	 *
	 * @param declaringType The declaring type for the members to process
	 * @param backingMemberConsumer Callback for members with a local `@Access`
	 */
	private static void collectAttributeLevelAccessMembers(
			ClassDetails declaringType,
			Consumer<MemberDetails> backingMemberConsumer) {
		for ( FieldDetails fieldDetails : declaringType.getFields() ) {
			if ( fieldDetails.hasAnnotation( Transient.class ) ) {
				continue;
			}

			final Access localAccess = fieldDetails.getAnnotation( Access.class );
			if ( localAccess == null ) {
				continue;
			}

			validateAttributeLevelAccess( fieldDetails, localAccess.value(), declaringType );

			backingMemberConsumer.accept( fieldDetails );
		}

		for ( MethodDetails methodDetails : declaringType.getMethods() ) {
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

			validateAttributeLevelAccess( methodDetails, localAccess.value(), declaringType );
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

	/**
	 * Perform an action for each member which matches the class-level access-type
	 *
	 * @param declaringType The declaring type for the members to process
	 * @param backingMemberConsumer Callback for members with a local `@Access`
	 */
	private static void collectClassLevelAccessMembers(
			AccessType classLevelAccessType,
			ClassDetails declaringType,
			Consumer<MemberDetails> backingMemberConsumer) {
		if ( classLevelAccessType == AccessType.FIELD ) {
			processClassLevelAccessFields( declaringType, backingMemberConsumer );
		}
		else {
			processClassLevelAccessMethods( declaringType, backingMemberConsumer );
		}
	}

	private static void processClassLevelAccessFields(
			ClassDetails declaringType,
			Consumer<MemberDetails> backingMemberConsumer) {
		for ( FieldDetails fieldDetails : declaringType.getFields() ) {
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
			ClassDetails declaringType,
			Consumer<MemberDetails> backingMemberConsumer) {
		for ( MethodDetails methodDetails : declaringType.getMethods() ) {
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
