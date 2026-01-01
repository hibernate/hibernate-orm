/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import jakarta.persistence.metamodel.Type;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldDescription.InDefinedShape;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.StubMethod;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.Version;
import org.hibernate.bytecode.enhance.VersionMismatchException;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.EnhancementInfo;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.bytecode.enhance.spi.UnloadedField;
import org.hibernate.bytecode.enhance.spi.UnsupportedEnhancementStrategy;
import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;
import org.hibernate.engine.spi.CompositeOwner;
import org.hibernate.engine.spi.Managed;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static java.lang.Character.isUpperCase;
import static java.lang.Character.toLowerCase;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Collections.emptyList;
import static net.bytebuddy.matcher.ElementMatchers.isGetter;
import static net.bytebuddy.matcher.ElementMatchers.isSetter;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static org.hibernate.bytecode.enhance.internal.BytecodeEnhancementLogging.ENHANCEMENT_LOGGER;
import static org.hibernate.bytecode.enhance.internal.bytebuddy.FeatureMismatchException.Feature.ASSOCIATION_MANAGEMENT;
import static org.hibernate.bytecode.enhance.internal.bytebuddy.FeatureMismatchException.Feature.DIRTY_CHECK;
import static org.hibernate.bytecode.enhance.internal.bytebuddy.ModelTypePool.buildModelTypePool;
import static org.hibernate.bytecode.enhance.internal.bytebuddy.PersistentAttributeTransformer.collectPersistentFields;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.ENTITY_INSTANCE_GETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.PERSISTENCE_INFO_SETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_CHANGER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_CLEAR_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_COLLECTION_CHANGED_FIELD_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_COLLECTION_CHANGED_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_COLLECTION_CLEAR_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_COLLECTION_GET_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_COLLECTION_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_COMPOSITE_CLEAR_OWNER;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_COMPOSITE_SET_OWNER;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_FIELD_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_GET_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_HAS_CHANGED_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_SUSPEND_NAME;
import static org.hibernate.engine.spi.ExtendedSelfDirtinessTracker.REMOVE_DIRTY_FIELDS_NAME;

public class EnhancerImpl implements Enhancer {

	final ByteBuddyEnhancementContext enhancementContext;
	private final ByteBuddyState byteBuddyState;
	private final EnhancerClassLocator typePool;
	private final EnhancerImplConstants constants;
	private final AnnotationList.ForLoadedAnnotations infoAnnotationList;

	/**
	 * Constructs the Enhancer, using the given context.
	 *
	 * @param enhancementContext Describes the context in which enhancement will occur so as to give access
	 * to contextual/environmental information.
	 * @param byteBuddyState refers to the ByteBuddy instance to use
	 */
	public EnhancerImpl(
			final EnhancementContext enhancementContext,
			final ByteBuddyState byteBuddyState) {
		this( enhancementContext, byteBuddyState,
				buildModelTypePool( enhancementContext.getLoadingClassLoader() ) );
	}

	/**
	 * Expert level constructor, this allows for more control of state and bytecode loading,
	 * which allows integrators to optimise for particular contexts of use.
	 */
	public EnhancerImpl(
			final EnhancementContext enhancementContext,
			final ByteBuddyState byteBuddyState,
			final EnhancerClassLocator classLocator) {
		this.enhancementContext =
				new ByteBuddyEnhancementContext( enhancementContext, byteBuddyState.getEnhancerConstants() );
		this.byteBuddyState = Objects.requireNonNull( byteBuddyState );
		this.typePool = Objects.requireNonNull( classLocator );
		this.constants = byteBuddyState.getEnhancerConstants();

		this.infoAnnotationList =
				new AnnotationList.ForLoadedAnnotations( List.of( createInfoAnnotation( enhancementContext ) ) );
	}


	/**
	 * Performs the enhancement.
	 *
	 * @param className The name of the class whose bytecode is being enhanced.
	 * @param originalBytes The class's original (pre-enhancement) byte code
	 *
	 * @return The enhanced bytecode. Could be the same as the original bytecode if the original was
	 * already enhanced or we could not enhance it for some reason.
	 *
	 * @throws EnhancementException Indicates a problem performing the enhancement
	 */
	@Override
	public byte[] enhance(String className, byte[] originalBytes) throws EnhancementException {
		//Classpool#describe does not accept '/' in the description name as it expects a class name. See HHH-12545
		final String safeClassName = className.replace( '/', '.' );
		typePool.registerClassNameAndBytes( safeClassName, originalBytes );
		try {
			final var typeDescription = typePool.describe( safeClassName ).resolve();
			return byteBuddyState.rewrite( typePool, safeClassName, byteBuddy ->
					doEnhance( () -> byteBuddy.ignore( constants.defaultFinalizer() )
									.redefine( typeDescription, typePool.asClassFileLocator() )
									.annotateType( infoAnnotationList ),
							typeDescription
					) );
		}
		catch (EnhancementException e) {
			throw e;
		}
		catch (RuntimeException e) {
			throw new EnhancementException( "Failed to enhance class " + className, e );
		}
		finally {
			typePool.deregisterClassNameAndBytes( safeClassName );
		}
	}

	@Override
	public void discoverTypes(String className, byte[] originalBytes) {
		if ( originalBytes != null ) {
			typePool.registerClassNameAndBytes( className, originalBytes );
		}
		try {
			final var typeDescription = typePool.describe( className ).resolve();
			enhancementContext.registerDiscoveredType( typeDescription, Type.PersistenceType.ENTITY );
			enhancementContext.discoverCompositeTypes( typeDescription, typePool );
		}
		catch (RuntimeException e) {
			throw new EnhancementException( "Failed to discover types for class " + className, e );
		}
		finally {
			typePool.deregisterClassNameAndBytes( className );
		}
	}

	private DynamicType.Builder<?> doEnhance(
			Supplier<DynamicType.Builder<?>> builderSupplier,
			TypeDescription managedCtClass) {
		if ( alreadyEnhanced( managedCtClass ) ) {
			// The class already implements `Managed`.
			// There are 2 broad cases:
			//		1. the user manually implemented `Managed`
			//		2. the class was previously enhanced
			// In either case, look for `@EnhancementInfo` and,
			// if found, verify we can "re-enhance" the class
			final var infoAnnotation =
					managedCtClass.getDeclaredAnnotations()
							.ofType( EnhancementInfo.class );
			if ( infoAnnotation != null ) {
				// throws an exception if there is a mismatch...
				verifyReEnhancement( managedCtClass, infoAnnotation.load(), enhancementContext );
			}
			// Verification succeeded (or not done)
			// We can simply skip the enhancement
			ENHANCEMENT_LOGGER.skippingAlreadyAnnotated( managedCtClass.getName() );
			return null;
		}
		// can't effectively enhance interfaces
		else if ( managedCtClass.isInterface() ) {
			ENHANCEMENT_LOGGER.skippingInterface( managedCtClass.getName() );
			return null;
		}
		// can't effectively enhance records
		else if ( managedCtClass.isRecord() ) {
			ENHANCEMENT_LOGGER.skippingRecord( managedCtClass.getName() );
			return null;
		}
		else if ( enhancementContext.isEntityClass( managedCtClass ) ) {
			if ( checkUnderlyingFields( managedCtClass, enhancementContext ) ) {
				// do not enhance classes with mismatched names for PROPERTY-access persistent attributes
				return null;
			}
			else {
				return createTransformer( managedCtClass )
						.applyTo( enhanceEntity( builderSupplier.get(), managedCtClass ) );
			}
		}
		else if ( enhancementContext.isCompositeClass( managedCtClass ) ) {
			if ( checkUnderlyingFields( managedCtClass, enhancementContext ) ) {
				// do not enhance classes with mismatched names for PROPERTY-access persistent attributes
				return null;
			}
			else {
				return createTransformer( managedCtClass )
						.applyTo( enhanceEmbeddable( builderSupplier.get(), managedCtClass ) );
			}
		}
		else if ( enhancementContext.isMappedSuperclassClass( managedCtClass ) ) {
			// Check for HHH-16572 (PROPERTY attributes with mismatched field and method names)
			if ( checkUnderlyingFields( managedCtClass, enhancementContext ) ) {
				return null;
			}
			else {
				ENHANCEMENT_LOGGER.enhancingAsMappedSuperclass( managedCtClass.getName() );
				final var builder = builderSupplier.get();
				builder.implement( constants.INTERFACES_for_ManagedMappedSuperclass );
				return createTransformer( managedCtClass ).applyTo( builder );
			}
		}
		else if ( enhancementContext.doExtendedEnhancement() ) {
			ENHANCEMENT_LOGGER.extendedEnhancement( managedCtClass.getName() );
			return createTransformer( managedCtClass ).applyExtended( builderSupplier.get() );
		}
		else {
			ENHANCEMENT_LOGGER.skippingNotEntityOrComposite( managedCtClass.getName() );
			return null;
		}
	}

	private DynamicType.Builder<?> enhanceEmbeddable(
			DynamicType.Builder<?> builder,
			TypeDescription embeddableClass) {
		ENHANCEMENT_LOGGER.enhancingAsComposite( embeddableClass.getName() );

		builder = builder.implement( constants.INTERFACES_for_ManagedComposite );
		builder = addInterceptorHandling( builder, embeddableClass );

		if ( enhancementContext.doDirtyCheckingInline() ) {
			return builder.implement( constants.INTERFACES_for_CompositeTracker )
					.defineField(
							TRACKER_COMPOSITE_FIELD_NAME,
							constants.TypeCompositeOwnerTracker,
							constants.modifierPRIVATE_TRANSIENT
					)
							.annotateField( constants.TRANSIENT_ANNOTATION )
					.defineMethod(
							TRACKER_COMPOSITE_SET_OWNER,
							constants.TypeVoid,
							constants.modifierPUBLIC
					)
							.withParameters( String.class, CompositeOwner.class )
							.intercept( constants.implementationSetOwner )
					.defineMethod(
							TRACKER_COMPOSITE_CLEAR_OWNER,
							constants.TypeVoid,
							constants.modifierPUBLIC
					)
							.withParameter( constants.TypeString )
							.intercept( constants.implementationClearOwner );
		}
		else {
			return builder;
		}
	}

	private DynamicType.Builder<?> enhanceEntity(
			DynamicType.Builder<?> builder,
			TypeDescription entityClass) {
		ENHANCEMENT_LOGGER.enhancingAsEntity( entityClass.getName() );

		builder = builder
				.implement( constants.INTERFACES_for_ManagedEntity )
				.defineMethod( ENTITY_INSTANCE_GETTER_NAME, constants.TypeObject, constants.modifierPUBLIC )
				.intercept( FixedValue.self() );

		builder = addFieldWithGetterAndSetter(
				builder,
				constants.TypeEntityEntry,
				EnhancerConstants.ENTITY_ENTRY_FIELD_NAME,
				EnhancerConstants.ENTITY_ENTRY_GETTER_NAME,
				EnhancerConstants.ENTITY_ENTRY_SETTER_NAME
		);
		builder = addFieldWithGetterAndSetter(
				builder,
				constants.TypeManagedEntity,
				EnhancerConstants.PREVIOUS_FIELD_NAME,
				EnhancerConstants.PREVIOUS_GETTER_NAME,
				EnhancerConstants.PREVIOUS_SETTER_NAME
		);
		builder = addFieldWithGetterAndSetter(
				builder,
				constants.TypeManagedEntity,
				EnhancerConstants.NEXT_FIELD_NAME,
				EnhancerConstants.NEXT_GETTER_NAME,
				EnhancerConstants.NEXT_SETTER_NAME
		);

		builder = addFieldWithGetterAndSetter(
				builder,
				constants.TypeBooleanPrimitive,
				EnhancerConstants.USE_TRACKER_FIELD_NAME,
				EnhancerConstants.USE_TRACKER_GETTER_NAME,
				EnhancerConstants.USE_TRACKER_SETTER_NAME
		);

		builder = addFieldWithGetterAndSetter(
				builder,
				constants.TypeIntegerPrimitive,
				EnhancerConstants.INSTANCE_ID_FIELD_NAME,
				EnhancerConstants.INSTANCE_ID_GETTER_NAME,
				EnhancerConstants.INSTANCE_ID_SETTER_NAME
		);

		builder = addSetPersistenceInfoMethod(
				builder,
				constants.TypeEntityEntry,
				constants.TypeManagedEntity,
				constants.TypeIntegerPrimitive
		);

		builder = addInterceptorHandling( builder, entityClass );

		if ( enhancementContext.doDirtyCheckingInline() ) {
			final var collectionFields = collectCollectionFields( entityClass );
			if ( collectionFields.isEmpty() ) {
				return implementSelfDirtinessTracker( builder );
			}
			else {
				//TODO es.enableInterfaceExtendedSelfDirtinessTracker?
				//     Careful with consequences.
				return enhanceCollectionFields( entityClass, collectionFields,
						implementExtendedSelfDirtinessTracker( builder ) );
			}
		}
		else {
			return builder;
		}
	}

	private DynamicType.Builder<?> implementExtendedSelfDirtinessTracker(DynamicType.Builder<?> builder) {
		return builder.implement( constants.INTERFACES_for_ExtendedSelfDirtinessTracker )
				.defineField( TRACKER_FIELD_NAME, constants.DirtyTrackerTypeDescription, constants.modifierPRIVATE_TRANSIENT )
						.annotateField( constants.TRANSIENT_ANNOTATION )
				.defineField( TRACKER_COLLECTION_NAME, constants.TypeCollectionTracker, constants.modifierPRIVATE_TRANSIENT )
						.annotateField( constants.TRANSIENT_ANNOTATION )
				.defineMethod( TRACKER_CHANGER_NAME, constants.TypeVoid, constants.modifierPUBLIC )
						.withParameter( constants.TypeString )
						.intercept( constants.implementationTrackChange )
				.defineMethod( TRACKER_GET_NAME, constants.Type_Array_String, constants.modifierPUBLIC )
						.intercept( constants.implementationGetDirtyAttributes )
				.defineMethod( TRACKER_HAS_CHANGED_NAME, constants.TypeBooleanPrimitive, constants.modifierPUBLIC )
						.intercept( constants.implementationAreFieldsDirty )
				.defineMethod( TRACKER_CLEAR_NAME, constants.TypeVoid, constants.modifierPUBLIC )
						.intercept( constants.implementationClearDirtyAttributes )
				.defineMethod( TRACKER_SUSPEND_NAME, constants.TypeVoid, constants.modifierPUBLIC )
						.withParameter( constants.TypeBooleanPrimitive )
						.intercept( constants.implementationSuspendDirtyTracking )
				.defineMethod( TRACKER_COLLECTION_GET_NAME, constants.TypeCollectionTracker, constants.modifierPUBLIC )
						.intercept( FieldAccessor.ofField( TRACKER_COLLECTION_NAME ) );
	}

	private DynamicType.Builder<?> implementSelfDirtinessTracker(DynamicType.Builder<?> builder) {
		return builder.implement( constants.INTERFACES_for_SelfDirtinessTracker )
				.defineField( TRACKER_FIELD_NAME, constants.DirtyTrackerTypeDescription, constants.modifierPRIVATE_TRANSIENT )
						.annotateField( constants.TRANSIENT_ANNOTATION )
				.defineMethod( TRACKER_CHANGER_NAME, constants.TypeVoid, constants.modifierPUBLIC )
						.withParameter( constants.TypeString )
						.intercept( constants.implementationTrackChange )
				.defineMethod( TRACKER_GET_NAME, constants.Type_Array_String, constants.modifierPUBLIC )
						.intercept( constants.implementationGetDirtyAttributesWithoutCollections )
				.defineMethod( TRACKER_HAS_CHANGED_NAME, constants.TypeBooleanPrimitive, constants.modifierPUBLIC )
						.intercept( constants.implementationAreFieldsDirtyWithoutCollections )
				.defineMethod( TRACKER_CLEAR_NAME, constants.TypeVoid, constants.modifierPUBLIC )
						.intercept( constants.implementationClearDirtyAttributesWithoutCollections )
				.defineMethod( TRACKER_SUSPEND_NAME, constants.TypeVoid, constants.modifierPUBLIC )
						.withParameter( constants.TypeBooleanPrimitive )
						.intercept( constants.implementationSuspendDirtyTracking )
				.defineMethod( TRACKER_COLLECTION_GET_NAME, constants.TypeCollectionTracker, constants.modifierPUBLIC )
						.intercept( constants.implementationGetCollectionTrackerWithoutCollections );
	}

	private DynamicType.Builder<?> enhanceCollectionFields(
			TypeDescription entityClass,
			List<AnnotatedFieldDescription> collectionFields,
			DynamicType.Builder<?> builder) {
		Implementation
				isDirty = StubMethod.INSTANCE,
				getDirtyNames = StubMethod.INSTANCE,
				clearDirtyNames = StubMethod.INSTANCE;
		for ( var collectionField : collectionFields ) {
			final String collectionFieldName = collectionField.getName();
			final Class<?> adviceIsDirty, adviceGetDirtyNames, adviceClearDirtyNames;
			if ( collectionField.getType().asErasure().isAssignableTo( Map.class ) ) {
				adviceIsDirty = CodeTemplates.MapAreCollectionFieldsDirty.class;
				adviceGetDirtyNames = CodeTemplates.MapGetCollectionFieldDirtyNames.class;
				adviceClearDirtyNames = CodeTemplates.MapGetCollectionClearDirtyNames.class;
			}
			else {
				adviceIsDirty = CodeTemplates.CollectionAreCollectionFieldsDirty.class;
				adviceGetDirtyNames = CodeTemplates.CollectionGetCollectionFieldDirtyNames.class;
				adviceClearDirtyNames = CodeTemplates.CollectionGetCollectionClearDirtyNames.class;
			}
			if ( collectionField.isVisibleTo( entityClass ) ) {
				final var fieldDescription = collectionField.getFieldDescription();
				isDirty = Advice.withCustomMapping()
						.bind( CodeTemplates.FieldName.class, collectionFieldName )
						.bind( CodeTemplates.FieldValue.class, fieldDescription )
						.to( adviceIsDirty, constants.adviceLocator )
						.wrap( isDirty );
				getDirtyNames = Advice.withCustomMapping()
						.bind( CodeTemplates.FieldName.class, collectionFieldName )
						.bind( CodeTemplates.FieldValue.class, fieldDescription )
						.to( adviceGetDirtyNames, constants.adviceLocator )
						.wrap( getDirtyNames );
				clearDirtyNames = Advice.withCustomMapping()
						.bind( CodeTemplates.FieldName.class, collectionFieldName )
						.bind( CodeTemplates.FieldValue.class, fieldDescription )
						.to( adviceClearDirtyNames, constants.adviceLocator )
						.wrap( clearDirtyNames );
			}
			else {
				final var getterMapping =
						new CodeTemplates.GetterMapping( collectionField.getFieldDescription() );
				isDirty = Advice.withCustomMapping()
						.bind( CodeTemplates.FieldName.class, collectionFieldName )
						.bind( CodeTemplates.FieldValue.class, getterMapping )
						.to( adviceIsDirty, constants.adviceLocator )
						.wrap( isDirty );
				getDirtyNames = Advice.withCustomMapping()
						.bind( CodeTemplates.FieldName.class, collectionFieldName )
						.bind( CodeTemplates.FieldValue.class, getterMapping )
						.to( adviceGetDirtyNames, constants.adviceLocator )
						.wrap( getDirtyNames );
				clearDirtyNames = Advice.withCustomMapping()
						.bind( CodeTemplates.FieldName.class, collectionFieldName )
						.bind( CodeTemplates.FieldValue.class, getterMapping )
						.to( adviceClearDirtyNames, constants.adviceLocator )
						.wrap( clearDirtyNames );
			}
		}

		if ( enhancementContext.hasLazyLoadableAttributes( entityClass ) ) {
			clearDirtyNames = constants.adviceInitializeLazyAttributeLoadingInterceptor.wrap( clearDirtyNames );
		}

		return builder.defineMethod( TRACKER_COLLECTION_CHANGED_NAME, constants.TypeBooleanPrimitive, constants.modifierPUBLIC )
				.intercept( isDirty )
				.defineMethod( TRACKER_COLLECTION_CHANGED_FIELD_NAME, constants.TypeVoid, constants.modifierPUBLIC )
						.withParameter( constants.DirtyTrackerTypeDescription )
						.intercept( getDirtyNames )
				.defineMethod( TRACKER_COLLECTION_CLEAR_NAME, constants.TypeVoid, constants.modifierPUBLIC )
						.intercept( Advice.withCustomMapping()
						.to( CodeTemplates.ClearDirtyCollectionNames.class, constants.adviceLocator )
						.wrap( StubMethod.INSTANCE ) )
				.defineMethod( REMOVE_DIRTY_FIELDS_NAME, constants.TypeVoid, constants.modifierPUBLIC )
						.withParameter( constants.TypeLazyAttributeLoadingInterceptor )
						.intercept( clearDirtyNames );
	}

	private void verifyReEnhancement(
			TypeDescription managedCtClass,
			EnhancementInfo existingInfo,
			ByteBuddyEnhancementContext enhancementContext) {
		// first, make sure versions match
		final String enhancementVersion = existingInfo.version();
		if ( "ignore".equals( enhancementVersion ) ) {
			// for testing
			ENHANCEMENT_LOGGER.skippingReEnhancementVersionCheck( managedCtClass.getName() );
		}
		else if ( !Version.getVersionString().equals( enhancementVersion ) ) {
			throw new VersionMismatchException( managedCtClass, enhancementVersion,
					Version.getVersionString() );
		}

		FeatureMismatchException.checkFeatureEnablement(
				managedCtClass,
				DIRTY_CHECK,
				enhancementContext.doDirtyCheckingInline(),
				existingInfo.includesDirtyChecking()
		);

		FeatureMismatchException.checkFeatureEnablement(
				managedCtClass,
				ASSOCIATION_MANAGEMENT,
				enhancementContext.doBiDirectionalAssociationManagement(),
				existingInfo.includesAssociationManagement()
		);
	}


	/**
	 * Utility that determines the access-type of a mapped class based on an explicit annotation
	 * or guessing it from the placement of its identifier property. Implementation should be
	 * aligned with {@code InheritanceState#determineDefaultAccessType()}.
	 *
	 * @return the {@link AccessType} used by the mapped class
	 *
	 * @implNote this does not fully account for embeddables, as they should inherit the access type
	 * from the entities they're used in - defaulting to PROPERTY to always run the accessor check
	 */
	private static AccessType determineDefaultAccessType(TypeDefinition typeDefinition) {
		for ( var candidate = typeDefinition;
			candidate != null && !candidate.represents( Object.class );
			candidate = candidate.getSuperClass() ) {
			final var annotations = candidate.asErasure().getDeclaredAnnotations();
			if ( hasMappingAnnotation( annotations ) ) {
				final var access = annotations.ofType( Access.class );
				if ( access != null ) {
					return access.load().value();
				}
			}
		}
		// Guess from identifier.
		// FIX: Shouldn't this be determined by the first attribute
		//      (i.e., field or property) with annotations,
		//      but without an explicit Access annotation, according
		//      to JPA 2.0 spec 2.3.1: Default Access Type?
		for ( var candidate = typeDefinition;
			candidate != null && !candidate.represents( Object.class );
			candidate = candidate.getSuperClass() ) {
			if ( hasMappingAnnotation( candidate.asErasure().getDeclaredAnnotations() ) ) {
				for ( var ctField : candidate.getDeclaredFields() ) {
					if ( !isStatic( ctField.getModifiers() )
						&& isIdField( ctField.getDeclaredAnnotations() ) ) {
						return AccessType.FIELD;
					}
				}
			}
		}
		// We can assume AccessType.PROPERTY here
		return AccessType.PROPERTY;
	}

	private static boolean isIdField(AnnotationList annotationList) {
		return annotationList.isAnnotationPresent( Id.class )
			|| annotationList.isAnnotationPresent( EmbeddedId.class );
	}

	/**
	 * Determines the access-type of the given annotation source if an explicit {@link Access}
	 * annotation is present, otherwise defaults to the provided {@code defaultAccessType}
	 */
	private static AccessType determineAccessType(AnnotationSource annotationSource, AccessType defaultAccessType) {
		final var access = annotationSource.getDeclaredAnnotations().ofType( Access.class );
		return access != null ? access.load().value() : defaultAccessType;
	}

	private static boolean hasMappingAnnotation(AnnotationList annotations) {
		return annotations.isAnnotationPresent( Entity.class )
			|| annotations.isAnnotationPresent( MappedSuperclass.class )
			|| annotations.isAnnotationPresent( Embeddable.class );
	}

	private static boolean isPersistentMethod(MethodDescription method) {
		final var annotations = method.getDeclaredAnnotations();
		return !annotations.isAnnotationPresent( Transient.class )
			&& annotations.stream()
				.noneMatch( a -> IGNORED_PERSISTENCE_ANNOTATIONS.contains( a.getAnnotationType().getName() ) );

	}

	private static final Set<String> IGNORED_PERSISTENCE_ANNOTATIONS = Set.of(
			"jakarta.persistence.PostLoad",
			"jakarta.persistence.PostPersist",
			"jakarta.persistence.PostRemove",
			"jakarta.persistence.PostUpdate",
			"jakarta.persistence.PrePersist",
			"jakarta.persistence.PreRemove",
			"jakarta.persistence.PreUpdate"
	);

	private static boolean containsField(Generic type, String fieldName) {
		do {
			if ( !type.getDeclaredFields()
					.filter( not( isStatic() ).and( named( fieldName ) ) )
					.isEmpty() ) {
				return true;
			}
			type = type.getSuperClass();
		}
		while ( type != null && !type.represents( Object.class ) );
		return false;
	}

	/// Currently, we cannot correctly handle cases where
	/// property accessor methods are not backed by a field
	/// with a matching name.
	///
	/// ```java
	/// @Entity
	/// class Book {
	///     Integer id; // Smiley
	///     String smtg; // Sad face
	///
	///     @Id Integer getId() { return id; }
	///     String getSomething() { return smtg; }
	/// }
	/// ```
	/// See [HHH-16572](https://hibernate.atlassian.net/browse/HHH-16572).
	///
	/// @return {@code true} if enhancement of the class must be
	///         [skipped](UnsupportedEnhancementStrategy#SKIP)
	///         because it has missing fields.
	///         {@code false} if enhancement of the class must proceed,
	///         because it doesn't have any missing fields, or because
	///         [legacy mode](UnsupportedEnhancementStrategy#LEGACY)
	///         was opted into.
	/// @throws EnhancementException if enhancement of the class must
	///         [abort](UnsupportedEnhancementStrategy#FAIL) because
	///         of missing fields.

	@SuppressWarnings("deprecation")
	private static boolean checkUnderlyingFields(
			TypeDescription managedCtClass,
			ByteBuddyEnhancementContext enhancementContext) {
		final var strategy = enhancementContext.getUnsupportedEnhancementStrategy();
		return switch ( strategy ) {
			case LEGACY ->
					// Don't check anything and act as if there was nothing unsupported
					// in the class. This is unsafe, but that's what LEGACY is about.
					false;
			default -> {
				final var methods =
						MethodGraph.Compiler.DEFAULT.compile( (TypeDefinition) managedCtClass )
								.listNodes()
								.asMethodList()
								.filter( isGetter().or( isSetter() ) );
				final var defaultAccessType = determineDefaultAccessType( managedCtClass );
				yield checkUnderlyingFields( managedCtClass, methods, defaultAccessType, strategy );
			}
		};
	}

	private static boolean checkUnderlyingFields(
			TypeDescription managedCtClass,
			MethodList<?> methods,
			AccessType defaultAccessType,
			UnsupportedEnhancementStrategy strategy) {
		for ( final var methodDescription : methods ) {
			if ( !methodDescription.getDeclaringType().represents( Object.class )
				// We only need to check this for AccessType.PROPERTY
				&& determineAccessType( methodDescription, defaultAccessType ) == AccessType.PROPERTY
				// We should ignore @Transient methods
				&& isPersistentMethod( methodDescription ) ) {
				final String fieldName = propertyName( methodDescription );
				if ( fieldName != null && !containsField( managedCtClass.asGenericType(), fieldName ) ) {
					// We shouldn't even be in this method if using LEGACY, see top of this method.
					return handleMissingField( managedCtClass, strategy, methodDescription, fieldName );
				}
			}
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	private static boolean handleMissingField(
			TypeDescription managedCtClass,
			UnsupportedEnhancementStrategy strategy,
			MethodDescription methodDescription,
			String fieldName) {
		return switch ( strategy ) {
			case SKIP -> {
				ENHANCEMENT_LOGGER.propertyAccessorNoFieldSkip(
						managedCtClass.getName(),
						fieldName,
						methodDescription.getName()
				);
				yield true;
			}
			case FAIL -> throw new EnhancementException( String.format(
					"Enhancement of [%s] failed because no underlying field named [%s] exists for property accessor method [%s]"
					+ " (ensure all property accessor methods have a matching field)",
					managedCtClass.getName(),
					fieldName,
					methodDescription.getName()
			) );
			case LEGACY -> throw new AssertionFailure( "Unexpected strategy at this point: " + strategy );
		};
	}

	private static @Nullable String propertyName(MethodDescription methodDescription) {
		return getJavaBeansFieldName( trimGetterName( methodDescription.getActualName() ) );
	}

	private static @NonNull String trimGetterName(String methodName) {
		if ( methodName.startsWith( "get" ) || methodName.startsWith( "set" ) ) {
			return methodName.substring( 3 );
		}
		else {
			assert methodName.startsWith( "is" );
			return methodName.substring( 2 );
		}
	}

	/**
	 * If the first two characters are upper case, assume all characters are upper case to be returned as is.
	 * Otherwise, return the name with the first character converted to lower case and the remaining part returned as is.
	 *
	 * @param name is the property accessor name to be updated following Persistence property name rules.
	 * @return name that follows JavaBeans rules, or {@code null} if the provided string is empty
	 */
	private static @Nullable String getJavaBeansFieldName(String name) {
		if ( name.isEmpty() ) {
			return null;
		}
		else if ( name.length() > 1
			&& isUpperCase( name.charAt( 1 ) )
			&& isUpperCase( name.charAt( 0 ) ) ) {
			return name;
		}
		else {
			final char[] chars = name.toCharArray();
			chars[0] = toLowerCase( chars[0] );
			return new String( chars );
		}
	}

	private PersistentAttributeTransformer createTransformer(TypeDescription typeDescription) {
		return collectPersistentFields( typeDescription, enhancementContext, typePool, constants );
	}

	// See HHH-10977 HHH-11284 HHH-11404 --- check for declaration of Managed interface on the class, not inherited
	private boolean alreadyEnhanced(TypeDescription managedCtClass) {
		for ( var declaredInterface : managedCtClass.getInterfaces() ) {
			if ( declaredInterface.asErasure().isAssignableTo( Managed.class ) ) {
				return true;
			}
		}
		return false;
	}

	private DynamicType.Builder<?> addInterceptorHandling(DynamicType.Builder<?> builder, TypeDescription managedCtClass) {
		// interceptor handling is only needed if the class has lazy-loadable attributes
		if ( enhancementContext.hasLazyLoadableAttributes( managedCtClass ) ) {
			ENHANCEMENT_LOGGER.weavingPersistentAttributeInterceptable( managedCtClass.getName() );
			return addFieldWithGetterAndSetter(
					builder.implement( constants.INTERFACES_for_PersistentAttributeInterceptable ),
					constants.TypePersistentAttributeInterceptor,
					EnhancerConstants.INTERCEPTOR_FIELD_NAME,
					EnhancerConstants.INTERCEPTOR_GETTER_NAME,
					EnhancerConstants.INTERCEPTOR_SETTER_NAME
			);
		}
		else {
			return builder;
		}
	}

	private DynamicType.Builder<?> addFieldWithGetterAndSetter(
			DynamicType.Builder<?> builder,
			TypeDefinition type,
			String fieldName,
			String getterName,
			String setterName) {
		return builder
				.defineField( fieldName, type, constants.modifierPRIVATE_TRANSIENT )
						.annotateField( constants.TRANSIENT_ANNOTATION )
				.defineMethod( getterName, type, constants.modifierPUBLIC )
						.intercept( FieldAccessor.ofField( fieldName ) )
				.defineMethod( setterName, constants.TypeVoid, constants.modifierPUBLIC )
						.withParameter( type )
						.intercept( FieldAccessor.ofField( fieldName ) );
	}

	private DynamicType.Builder<?> addSetPersistenceInfoMethod(
			DynamicType.Builder<?> builder,
			TypeDefinition entityEntryType,
			TypeDefinition managedEntityType,
			TypeDefinition intType) {
		return builder
				// returns previous entity entry
				.defineMethod( PERSISTENCE_INFO_SETTER_NAME, entityEntryType, constants.modifierPUBLIC )
				// previous, next, instance-id
				.withParameters( entityEntryType, managedEntityType, managedEntityType, intType )
				.intercept( constants.implementationSetPersistenceInfo );
	}

	private List<AnnotatedFieldDescription> collectCollectionFields(TypeDescription managedCtClass) {
		final List<AnnotatedFieldDescription> collectionList = new ArrayList<>();
		for ( var ctField : managedCtClass.getDeclaredFields() ) {
			// skip static fields and skip fields added by enhancement
			if ( !isStatic( ctField.getModifiers() )
				&& !ctField.getName().startsWith( "$$_hibernate_" ) ) {
				final var annotatedField = new AnnotatedFieldDescription( enhancementContext, ctField );
				if ( enhancementContext.isPersistentField( annotatedField )
					&& enhancementContext.isMappedCollection( annotatedField ) ) {
					final var erasure = ctField.getType().asErasure();
					if ( erasure.isAssignableTo( Collection.class )
						|| erasure.isAssignableTo( Map.class ) ) {
						collectionList.add( annotatedField );
					}
				}
			}
		}
		// HHH-10646 Add fields inherited from @MappedSuperclass
		// HHH-10981 There is no need to do it for @MappedSuperclass
		if ( !enhancementContext.isMappedSuperclassClass( managedCtClass ) ) {
			collectionList.addAll( collectInheritCollectionFields( managedCtClass ) );
		}
		return collectionList;
	}

	private Collection<AnnotatedFieldDescription> collectInheritCollectionFields(TypeDefinition managedCtClass) {
		final var managedCtSuperclass = managedCtClass.getSuperClass();
		if ( managedCtSuperclass == null || managedCtSuperclass.represents( Object.class ) ) {
			return emptyList();
		}

		if ( !enhancementContext.isMappedSuperclassClass( managedCtSuperclass.asErasure() ) ) {
			return collectInheritCollectionFields( managedCtSuperclass.asErasure() );
		}
		final List<AnnotatedFieldDescription> collectionList = new ArrayList<>();
		for ( var ctField : managedCtSuperclass.getDeclaredFields() ) {
			if ( !isStatic( ctField.getModifiers() ) ) {
				final var annotatedField = new AnnotatedFieldDescription( enhancementContext, ctField );
				if ( enhancementContext.isPersistentField( annotatedField )
					&& enhancementContext.isMappedCollection( annotatedField ) ) {
					final var erasure = ctField.getType().asErasure();
					if ( erasure.isAssignableTo( Collection.class )
						|| erasure.isAssignableTo( Map.class ) ) {
					collectionList.add( annotatedField );
					}
				}
			}
		}
		collectionList.addAll( collectInheritCollectionFields( managedCtSuperclass ) );
		return collectionList;
	}

	static String capitalize(String value) {
		return Character.toUpperCase( value.charAt( 0 ) ) + value.substring( 1 );
	}

	static class AnnotatedFieldDescription implements UnloadedField {

		private final ByteBuddyEnhancementContext context;

		private final FieldDescription fieldDescription;

		private AnnotationList annotations;

		private Optional<MethodDescription> getter;

		AnnotatedFieldDescription(ByteBuddyEnhancementContext context, FieldDescription fieldDescription) {
			this.context = context;
			this.fieldDescription = fieldDescription;
		}

		@Override
		public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
			return getAnnotations().isAnnotationPresent( annotationType );
		}

		@Override
		public String toString() {
			return fieldDescription.toString();
		}

		<T extends Annotation> AnnotationDescription.Loadable<T> getAnnotation(Class<T> annotationType) {
			return getAnnotations().ofType( annotationType );
		}

		String getName() {
			return fieldDescription.getName();
		}

		TypeDefinition getDeclaringType() {
			return fieldDescription.getDeclaringType();
		}

		Generic getType() {
			return fieldDescription.getType();
		}

		InDefinedShape asDefined() {
			return fieldDescription.asDefined();
		}

		String getDescriptor() {
			return fieldDescription.getDescriptor();
		}

		boolean isVisibleTo(TypeDescription type) {
			final var declaringType = fieldDescription.getDeclaringType().asErasure();
			if ( declaringType.isVisibleTo( type ) ) {
				if ( fieldDescription.isPublic() || type.equals( declaringType ) ) {
					return true;
				}
				else if ( fieldDescription.isProtected() ) {
					return declaringType.isAssignableFrom( type );
				}
				else if ( fieldDescription.isPrivate() ) {
					return type.isNestMateOf( declaringType );
				}
				// We explicitly consider package-private fields as not visible, as the classes
				// might have the same package name but be loaded by different class loaders.
				// (see https://hibernate.atlassian.net/browse/HHH-19784)
			}
			return false;
		}

		FieldDescription getFieldDescription() {
			return fieldDescription;
		}

		Optional<MethodDescription> getGetter() {
			if ( getter == null ) {
				getter = context.resolveGetter( fieldDescription );
			}
			return getter;
		}

		private AnnotationList getAnnotations() {
			if ( annotations == null ) {
				annotations = doGetAnnotations();
			}
			return annotations;
		}

		private AnnotationList doGetAnnotations() {
			final var access =
					fieldDescription.getDeclaringType().asErasure()
							.getDeclaredAnnotations().ofType( Access.class );
			if ( access != null && access.load().value() == AccessType.PROPERTY ) {
				var getter = getGetter();
				return getter.isPresent()
						? getter.get().getDeclaredAnnotations()
						: fieldDescription.getDeclaredAnnotations();
			}
			else if ( access != null && access.load().value() == AccessType.FIELD ) {
				return fieldDescription.getDeclaredAnnotations();
			}
			else {
				final var getter = getGetter();
				// Note that the order here is important
				final List<AnnotationDescription> annotationDescriptions = new ArrayList<>();
				if ( getter.isPresent() ) {
					annotationDescriptions.addAll( getter.get().getDeclaredAnnotations() );
				}
				annotationDescriptions.addAll( fieldDescription.getDeclaredAnnotations() );
				return new AnnotationList.Explicit( annotationDescriptions );
			}
		}
	}

	private static EnhancementInfo createInfoAnnotation(EnhancementContext enhancementContext) {
		return new EnhancementInfoImpl( enhancementContext.doDirtyCheckingInline(),
				enhancementContext.doBiDirectionalAssociationManagement() );
	}

	private static class EnhancementInfoImpl implements EnhancementInfo {
		private final String version;
		private final boolean includesDirtyChecking;
		private final boolean includesAssociationManagement;

		public EnhancementInfoImpl(boolean includesDirtyChecking, boolean includesAssociationManagement) {
			this.version = Version.getVersionString();
			this.includesDirtyChecking = includesDirtyChecking;
			this.includesAssociationManagement = includesAssociationManagement;
		}

		@Override
		public String version() {
			return version;
		}

		@Override
		public boolean includesDirtyChecking() {
			return includesDirtyChecking;
		}

		@Override
		public boolean includesAssociationManagement() {
			return includesAssociationManagement;
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return EnhancementInfo.class;
		}
	}

}
