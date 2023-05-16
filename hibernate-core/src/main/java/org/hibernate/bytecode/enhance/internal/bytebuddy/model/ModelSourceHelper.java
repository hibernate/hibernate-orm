/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.impl.ManagedTypeDescriptorImpl;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.impl.PersistentAttributeFactory;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Transient;
import net.bytebuddy.dynamic.ClassFileLocator;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.hibernate.bytecode.enhance.internal.bytebuddy.model.ModelSourceLogging.MODEL_SOURCE_LOGGER;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * @author Steve Ebersole
 */
public class ModelSourceHelper {
	public static ManagedTypeDescriptor buildManagedTypeDescriptor(
			ClassDetails declaringType,
			AccessType contextAccessType,
			ManagedTypeModelContext processingContext) {
		return new ManagedTypeDescriptorImpl(
				declaringType,
				buildPersistentAttributeList( declaringType, contextAccessType, processingContext ),
				processingContext
		);
	}
	public static List<PersistentAttribute> buildPersistentAttributeList(
			ClassDetails declaringType,
			AccessType contextAccessType,
			ManagedTypeModelContext processingContext) {
		final AccessType classLevelAccessType = determineClassLevelAccessType(
				declaringType,
				declaringType.getIdentifierMember(),
				contextAccessType
		);

		MODEL_SOURCE_LOGGER.debugf( "Building PersistentAttribute list for %s using %s class-level access", declaringType.getName(), classLevelAccessType );

		final LinkedHashMap<String,FieldDetails> fieldsByName = new LinkedHashMap<>();
		final LinkedHashMap<String,MethodDetails> gettersByAttributeName = new LinkedHashMap<>();
		final LinkedHashMap<String,MethodDetails> settersByAttributeName = new LinkedHashMap<>();

		final LinkedHashMap<String,MemberDetails> attributeMembers = collectBackingMembers(
				declaringType,
				classLevelAccessType,
				fieldsByName::put,
				gettersByAttributeName::put,
				settersByAttributeName::put
		);

		final List<PersistentAttribute> attributes = arrayList( attributeMembers.size() );
		ClassNode classNode = null;
		Map<String, MethodNode> getterMethodNodeMap = null;
		for ( Map.Entry<String, MemberDetails> membersEntry : attributeMembers.entrySet() ) {
			final PersistentAttribute attributeDescriptor = buildPersistentAttribute(
					membersEntry.getKey(),
					membersEntry.getValue(),
					fieldsByName,
					gettersByAttributeName,
					settersByAttributeName,
					declaringType,
					processingContext
			);
			attributes.add( attributeDescriptor );

			if ( attributeDescriptor.getUnderlyingField() == null ) {
				// we need the field,  but it is not known likely due to a naming mismatch (see HHH-16572).
				// locate it using ASM
				assert attributeDescriptor.getUnderlyingGetter() != null;
				if ( classNode == null ) {
					classNode = buildClassNode( declaringType, processingContext );
					getterMethodNodeMap = buildGetterMethodNodeMap( declaringType, classNode, processingContext );
				}

				final FieldDetails backingField = locateBackingField(
						attributeDescriptor,
						getterMethodNodeMap,
						fieldsByName
				);
				attributeDescriptor.setUnderlyingField( backingField );
			}
		}

		return attributes;
	}

	private static FieldDetails locateBackingField(
			PersistentAttribute attributeDescriptor,
			Map<String, MethodNode> getterMethodNodeMap,
			Map<String, FieldDetails> fieldDetailsMap) {
		final MethodNode methodNode = getterMethodNodeMap.get( attributeDescriptor.getName() );

		// assume the final GETFIELD instruction is the backing field
		for ( int i = methodNode.instructions.size() - 1; i >= 0; i-- ) {
			final AbstractInsnNode instruction = methodNode.instructions.get( i );
			if ( instruction.getOpcode() == Opcodes.GETFIELD ) {
				final FieldInsnNode getFieldInstruction = (FieldInsnNode) instruction;
				final String returnedFieldName = getFieldInstruction.name;
				return fieldDetailsMap.get( returnedFieldName );
			}
		}

		return null;
	}

	private static Map<String,MethodNode> buildGetterMethodNodeMap(
			ClassDetails declaringType,
			ClassNode classNode,
			ManagedTypeModelContext processingContext) {
		final LinkedHashMap<String,MethodNode> methodNodesByAttributeName = new LinkedHashMap<>();
		final LinkedHashMap<String,MethodDetails> getterMethodDetailsByName = new LinkedHashMap<>();
		for ( MethodDetails method : declaringType.getMethods() ) {
			if ( method.getMethodKind() != MethodDetails.MethodKind.GETTER ) {
				continue;
			}

			getterMethodDetailsByName.put( method.getName(), method );
		}

		for ( MethodNode methodNode : classNode.methods ) {
			final MethodDetails getterMethodDetails = getterMethodDetailsByName.get( methodNode.name );
			if ( getterMethodDetails == null ) {
				continue;
			}

			methodNodesByAttributeName.put( getterMethodDetails.resolveAttributeName(), methodNode );
		}

		return methodNodesByAttributeName;
	}

	private static ClassNode buildClassNode(ClassDetails declaringType, ManagedTypeModelContext processingContext) {
		try {
			final ClassNode classNode = new ClassNode();
			final ClassFileLocator.Resolution resolution = processingContext
					.getModelProcessingContext()
					.getClassFileLocator()
					.locate( declaringType.getClassName() );
			final ClassReader classReader = new ClassReader( resolution.resolve() );
			classReader.accept( classNode, ClassReader.SKIP_DEBUG );
			return classNode;
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

	private static PersistentAttribute buildPersistentAttribute(
			String attributeName,
			MemberDetails backingMemberDetails,
			Map<String,FieldDetails> fieldsByName,
			Map<String,MethodDetails> gettersByAttributeName,
			Map<String,MethodDetails> settersByAttributeName,
			ClassDetails declaringType,
			ManagedTypeModelContext processingContext) {
		assert backingMemberDetails.getKind() == AnnotationTarget.Kind.FIELD
				|| backingMemberDetails.getKind() == AnnotationTarget.Kind.METHOD;

		if ( backingMemberDetails.getKind() == AnnotationTarget.Kind.FIELD ) {
			final PersistentAttributeFactory builder = new PersistentAttributeFactory(
					declaringType,
					attributeName,
					AccessType.FIELD,
					(FieldDetails) backingMemberDetails,
					gettersByAttributeName.get( attributeName ),
					settersByAttributeName.get( attributeName )
			);

			return builder.buildPersistentAttribute();
		}

		assert backingMemberDetails.getKind() == AnnotationTarget.Kind.METHOD;
		final PersistentAttributeFactory builder = new PersistentAttributeFactory(
				declaringType,
				attributeName,
				AccessType.PROPERTY,
				fieldsByName.get( attributeName ),
				(MethodDetails) backingMemberDetails,
				settersByAttributeName.get( attributeName )
		);

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

		return contextAccessType == null ? AccessType.PROPERTY : contextAccessType;
	}

	public static LinkedHashMap<String,MemberDetails> collectBackingMembers(
			ClassDetails declaringType,
			AccessType classLevelAccessType,
			BiConsumer<String,FieldDetails> fieldCollector,
			BiConsumer<String,MethodDetails> getterCollector,
			BiConsumer<String,MethodDetails> setterCollector) {
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

		collectAttributeLevelAccessMembers( declaringType, backingMemberConsumer, fieldCollector, getterCollector, setterCollector );
		collectClassLevelAccessMembers( classLevelAccessType, declaringType, backingMemberConsumer );

		return attributeMembers;
	}

	/**
	 * Perform an action for each member which locally define an `AccessType` via `@Access`.
	 * <p/>
	 * This method visits each method and field on the type, so we use this as an opportunity
	 * to collect all fields, getters and setters for use later in building PersistentAttribute
	 * references
	 *
	 * @param declaringType The declaring type for the members to process
	 * @param backingMemberConsumer Callback for members with a local `@Access`
	 */
	private static void collectAttributeLevelAccessMembers(
			ClassDetails declaringType,
			Consumer<MemberDetails> backingMemberConsumer,
			BiConsumer<String, FieldDetails> fieldCollector,
			BiConsumer<String, MethodDetails> getterCollector,
			BiConsumer<String, MethodDetails> setterCollector) {
		for ( FieldDetails fieldDetails : declaringType.getFields() ) {
			fieldCollector.accept( fieldDetails.resolveAttributeName(), fieldDetails );

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
			if ( methodDetails.getMethodKind() == MethodDetails.MethodKind.GETTER ) {
				getterCollector.accept( methodDetails.resolveAttributeName(), methodDetails );
			}
			else {
				if ( methodDetails.getMethodKind() != MethodDetails.MethodKind.SETTER ) {
					setterCollector.accept( methodDetails.resolveAttributeName(), methodDetails );
				}
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
