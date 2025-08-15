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
import org.hibernate.engine.spi.ExtendedSelfDirtinessTracker;
import org.hibernate.engine.spi.Managed;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static net.bytebuddy.matcher.ElementMatchers.isGetter;
import static net.bytebuddy.matcher.ElementMatchers.isSetter;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static org.hibernate.bytecode.enhance.internal.bytebuddy.FeatureMismatchException.Feature.ASSOCIATION_MANAGEMENT;
import static org.hibernate.bytecode.enhance.internal.bytebuddy.FeatureMismatchException.Feature.DIRTY_CHECK;

public class EnhancerImpl implements Enhancer {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( Enhancer.class );

	protected final ByteBuddyEnhancementContext enhancementContext;
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
	public EnhancerImpl(final EnhancementContext enhancementContext, final ByteBuddyState byteBuddyState) {
		this( enhancementContext, byteBuddyState, ModelTypePool.buildModelTypePool( enhancementContext.getLoadingClassLoader() ) );
	}

	/**
	 * Expert level constructor, this allows for more control of state and bytecode loading,
	 * which allows integrators to optimise for particular contexts of use.
	 * @param enhancementContext
	 * @param byteBuddyState
	 * @param classLocator
	 */
	public EnhancerImpl(final EnhancementContext enhancementContext, final ByteBuddyState byteBuddyState, final EnhancerClassLocator classLocator) {
		this.enhancementContext = new ByteBuddyEnhancementContext( enhancementContext, byteBuddyState.getEnhancerConstants() );
		this.byteBuddyState = Objects.requireNonNull( byteBuddyState );
		this.typePool = Objects.requireNonNull( classLocator );
		this.constants = byteBuddyState.getEnhancerConstants();

		this.infoAnnotationList = new AnnotationList.ForLoadedAnnotations( List.of( createInfoAnnotation( enhancementContext ) ) );
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
			final TypeDescription typeDescription = typePool.describe( safeClassName ).resolve();

			return byteBuddyState.rewrite( typePool, safeClassName, byteBuddy -> doEnhance(
					() -> byteBuddy.ignore( constants.defaultFinalizer() )
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
			final TypeDescription typeDescription = typePool.describe( className ).resolve();
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

	private DynamicType.Builder<?> doEnhance(Supplier<DynamicType.Builder<?>> builderSupplier, TypeDescription managedCtClass) {
		if ( alreadyEnhanced( managedCtClass ) ) {
			// the class already implements `Managed`.  there are 2 broad cases -
			//		1. the user manually implemented `Managed`
			//		2. the class was previously enhanced
			// in either case, look for `@EnhancementInfo` and, if found, verify we can "re-enhance" the class
			final AnnotationDescription.Loadable<EnhancementInfo> infoAnnotation = managedCtClass.getDeclaredAnnotations().ofType( EnhancementInfo.class );
			if ( infoAnnotation != null ) {
				// throws an exception if there is a mismatch...
				verifyReEnhancement( managedCtClass, infoAnnotation.load(), enhancementContext );
			}
			// verification succeeded (or not done) - we can simply skip the enhancement
			log.tracef( "Skipping enhancement of [%s]: it's already annotated with @EnhancementInfo", managedCtClass.getName() );
			return null;
		}

		// can't effectively enhance interfaces
		if ( managedCtClass.isInterface() ) {
			log.tracef( "Skipping enhancement of [%s]: it's an interface", managedCtClass.getName() );
			return null;
		}

		// can't effectively enhance records
		if ( managedCtClass.isRecord() ) {
			log.tracef( "Skipping enhancement of [%s]: it's a record", managedCtClass.getName() );
			return null;
		}

		if ( enhancementContext.isEntityClass( managedCtClass ) ) {
			if ( checkUnsupportedAttributeNaming( managedCtClass, enhancementContext ) ) {
				// do not enhance classes with mismatched names for PROPERTY-access persistent attributes
				return null;
			}

			log.tracef( "Enhancing [%s] as Entity", managedCtClass.getName() );
			DynamicType.Builder<?> builder = builderSupplier.get();
			builder = builder
					.implement( constants.INTERFACES_for_ManagedEntity )
					.defineMethod( EnhancerConstants.ENTITY_INSTANCE_GETTER_NAME, constants.TypeObject, constants.modifierPUBLIC )
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

			builder = addInterceptorHandling( builder, managedCtClass );

			if ( enhancementContext.doDirtyCheckingInline() ) {
				List<AnnotatedFieldDescription> collectionFields = collectCollectionFields( managedCtClass );

				if ( collectionFields.isEmpty() ) {
					builder = builder.implement( constants.INTERFACES_for_SelfDirtinessTracker )
							.defineField( EnhancerConstants.TRACKER_FIELD_NAME, constants.DirtyTrackerTypeDescription, constants.modifierPRIVATE_TRANSIENT )
									.annotateField( constants.TRANSIENT_ANNOTATION )
							.defineMethod( EnhancerConstants.TRACKER_CHANGER_NAME, constants.TypeVoid, constants.modifierPUBLIC )
									.withParameter( constants.TypeString )
									.intercept( constants.implementationTrackChange )
							.defineMethod( EnhancerConstants.TRACKER_GET_NAME, constants.Type_Array_String, constants.modifierPUBLIC )
									.intercept( constants.implementationGetDirtyAttributesWithoutCollections )
							.defineMethod( EnhancerConstants.TRACKER_HAS_CHANGED_NAME, constants.TypeBooleanPrimitive, constants.modifierPUBLIC )
									.intercept( constants.implementationAreFieldsDirtyWithoutCollections )
							.defineMethod( EnhancerConstants.TRACKER_CLEAR_NAME, constants.TypeVoid, constants.modifierPUBLIC )
									.intercept( constants.implementationClearDirtyAttributesWithoutCollections )
							.defineMethod( EnhancerConstants.TRACKER_SUSPEND_NAME, constants.TypeVoid, constants.modifierPUBLIC )
									.withParameter( constants.TypeBooleanPrimitive )
									.intercept( constants.implementationSuspendDirtyTracking )
							.defineMethod( EnhancerConstants.TRACKER_COLLECTION_GET_NAME, constants.TypeCollectionTracker, constants.modifierPUBLIC )
									.intercept( constants.implementationGetCollectionTrackerWithoutCollections );
				}
				else {
					//TODO es.enableInterfaceExtendedSelfDirtinessTracker ? Careful with consequences..
					builder = builder.implement( constants.INTERFACES_for_ExtendedSelfDirtinessTracker )
							.defineField( EnhancerConstants.TRACKER_FIELD_NAME, constants.DirtyTrackerTypeDescription, constants.modifierPRIVATE_TRANSIENT )
									.annotateField( constants.TRANSIENT_ANNOTATION )
							.defineField( EnhancerConstants.TRACKER_COLLECTION_NAME, constants.TypeCollectionTracker, constants.modifierPRIVATE_TRANSIENT )
									.annotateField( constants.TRANSIENT_ANNOTATION )
							.defineMethod( EnhancerConstants.TRACKER_CHANGER_NAME, constants.TypeVoid, constants.modifierPUBLIC )
									.withParameter( constants.TypeString )
									.intercept( constants.implementationTrackChange )
							.defineMethod( EnhancerConstants.TRACKER_GET_NAME, constants.Type_Array_String, constants.modifierPUBLIC )
									.intercept( constants.implementationGetDirtyAttributes )
							.defineMethod( EnhancerConstants.TRACKER_HAS_CHANGED_NAME, constants.TypeBooleanPrimitive, constants.modifierPUBLIC )
									.intercept( constants.implementationAreFieldsDirty )
							.defineMethod( EnhancerConstants.TRACKER_CLEAR_NAME, constants.TypeVoid, constants.modifierPUBLIC )
									.intercept( constants.implementationClearDirtyAttributes )
							.defineMethod( EnhancerConstants.TRACKER_SUSPEND_NAME, constants.TypeVoid, constants.modifierPUBLIC )
									.withParameter( constants.TypeBooleanPrimitive )
									.intercept( constants.implementationSuspendDirtyTracking )
							.defineMethod( EnhancerConstants.TRACKER_COLLECTION_GET_NAME, constants.TypeCollectionTracker, constants.modifierPUBLIC )
									.intercept( FieldAccessor.ofField( EnhancerConstants.TRACKER_COLLECTION_NAME ) );

					Implementation isDirty = StubMethod.INSTANCE, getDirtyNames = StubMethod.INSTANCE, clearDirtyNames = StubMethod.INSTANCE;
					for ( AnnotatedFieldDescription collectionField : collectionFields ) {
						String collectionFieldName = collectionField.getName();
						Class adviceIsDirty;
						Class adviceGetDirtyNames;
						Class adviceClearDirtyNames;
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
						if ( collectionField.isVisibleTo( managedCtClass ) ) {
							FieldDescription fieldDescription = collectionField.getFieldDescription();
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
							CodeTemplates.GetterMapping getterMapping = new CodeTemplates.GetterMapping(
									collectionField.getFieldDescription() );
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

					if ( enhancementContext.hasLazyLoadableAttributes( managedCtClass ) ) {
						clearDirtyNames = constants.adviceInitializeLazyAttributeLoadingInterceptor.wrap( clearDirtyNames );
					}

					builder = builder.defineMethod( EnhancerConstants.TRACKER_COLLECTION_CHANGED_NAME, constants.TypeBooleanPrimitive, constants.modifierPUBLIC )
							.intercept( isDirty )
							.defineMethod( EnhancerConstants.TRACKER_COLLECTION_CHANGED_FIELD_NAME, constants.TypeVoid, constants.modifierPUBLIC )
									.withParameter( constants.DirtyTrackerTypeDescription )
									.intercept( getDirtyNames )
							.defineMethod( EnhancerConstants.TRACKER_COLLECTION_CLEAR_NAME, constants.TypeVoid, constants.modifierPUBLIC )
									.intercept( Advice.withCustomMapping()
									.to( CodeTemplates.ClearDirtyCollectionNames.class, constants.adviceLocator )
									.wrap( StubMethod.INSTANCE ) )
							.defineMethod( ExtendedSelfDirtinessTracker.REMOVE_DIRTY_FIELDS_NAME, constants.TypeVoid, constants.modifierPUBLIC )
									.withParameter( constants.TypeLazyAttributeLoadingInterceptor )
									.intercept( clearDirtyNames );
				}
			}

			return createTransformer( managedCtClass ).applyTo( builder );
		}
		else if ( enhancementContext.isCompositeClass( managedCtClass ) ) {
			if ( checkUnsupportedAttributeNaming( managedCtClass, enhancementContext ) ) {
				// do not enhance classes with mismatched names for PROPERTY-access persistent attributes
				return null;
			}

			log.tracef( "Enhancing [%s] as Composite", managedCtClass.getName() );

			DynamicType.Builder<?> builder = builderSupplier.get();
			builder = builder.implement( constants.INTERFACES_for_ManagedComposite );
			builder = addInterceptorHandling( builder, managedCtClass );

			if ( enhancementContext.doDirtyCheckingInline() ) {
				builder = builder.implement( constants.INTERFACES_for_CompositeTracker )
						.defineField(
								EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME,
								constants.TypeCompositeOwnerTracker,
								constants.modifierPRIVATE_TRANSIENT
						)
								.annotateField( constants.TRANSIENT_ANNOTATION )
						.defineMethod(
								EnhancerConstants.TRACKER_COMPOSITE_SET_OWNER,
								constants.TypeVoid,
								constants.modifierPUBLIC
						)
								.withParameters( String.class, CompositeOwner.class )
								.intercept( constants.implementationSetOwner )
						.defineMethod(
								EnhancerConstants.TRACKER_COMPOSITE_CLEAR_OWNER,
								constants.TypeVoid,
								constants.modifierPUBLIC
						)
								.withParameter( constants.TypeString )
								.intercept( constants.implementationClearOwner );
			}

			return createTransformer( managedCtClass ).applyTo( builder );
		}
		else if ( enhancementContext.isMappedSuperclassClass( managedCtClass ) ) {
			// Check for HHH-16572 (PROPERTY attributes with mismatched field and method names)
			if ( checkUnsupportedAttributeNaming( managedCtClass, enhancementContext ) ) {
				return null;
			}

			log.tracef( "Enhancing [%s] as MappedSuperclass", managedCtClass.getName() );

			DynamicType.Builder<?> builder = builderSupplier.get();
			builder = builder.implement( constants.INTERFACES_for_ManagedMappedSuperclass );
			return createTransformer( managedCtClass ).applyTo( builder );
		}
		else if ( enhancementContext.doExtendedEnhancement() ) {
			log.tracef( "Extended enhancement of [%s]", managedCtClass.getName() );
			return createTransformer( managedCtClass ).applyExtended( builderSupplier.get() );
		}
		else {
			log.tracef( "Skipping enhancement of [%s]: not entity or composite", managedCtClass.getName() );
			return null;
		}
	}

	private void verifyReEnhancement(
			TypeDescription managedCtClass,
			EnhancementInfo existingInfo,
			ByteBuddyEnhancementContext enhancementContext) {
		// first, make sure versions match
		final String enhancementVersion = existingInfo.version();
		if ( "ignore".equals( enhancementVersion ) ) {
			// for testing
			log.debugf( "Skipping re-enhancement version check for %s due to `ignore`", managedCtClass.getName() );
		}
		else if ( !Version.getVersionString().equals( enhancementVersion ) ) {
			throw new VersionMismatchException( managedCtClass, enhancementVersion, Version.getVersionString() );
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
	 * @implNote this does not fully account for embeddables, as they should inherit the access-type
	 * from the entities they're used in - defaulting to PROPERTY to always run the accessor check
	 */
	private static AccessType determineDefaultAccessType(TypeDefinition typeDefinition) {
		for ( TypeDefinition candidate = typeDefinition; candidate != null && !candidate.represents( Object.class ); candidate = candidate.getSuperClass() ) {
			final AnnotationList annotations = candidate.asErasure().getDeclaredAnnotations();
			if ( hasMappingAnnotation( annotations ) ) {
				final AnnotationDescription.Loadable<Access> access = annotations.ofType( Access.class );
				if ( access != null ) {
					return access.load().value();
				}
			}
		}
		// Guess from identifier.
		// FIX: Shouldn't this be determined by the first attribute (i.e., field or property) with annotations,
		// but without an explicit Access annotation, according to JPA 2.0 spec 2.3.1: Default Access Type?
		for ( TypeDefinition candidate = typeDefinition; candidate != null && !candidate.represents( Object.class ); candidate = candidate.getSuperClass() ) {
			final AnnotationList annotations = candidate.asErasure().getDeclaredAnnotations();
			if ( hasMappingAnnotation( annotations ) ) {
				for ( FieldDescription ctField : candidate.getDeclaredFields() ) {
					if ( !Modifier.isStatic( ctField.getModifiers() ) ) {
						final AnnotationList annotationList = ctField.getDeclaredAnnotations();
						if ( annotationList.isAnnotationPresent( Id.class ) || annotationList.isAnnotationPresent( EmbeddedId.class ) ) {
							return AccessType.FIELD;
						}
					}
				}
			}
		}
		// We can assume AccessType.PROPERTY here
		return AccessType.PROPERTY;
	}

	/**
	 * Determines the access-type of the given annotation source if an explicit {@link Access} annotation
	 * is present, otherwise defaults to the provided {@code defaultAccessType}
	 */
	private static AccessType determineAccessType(AnnotationSource annotationSource, AccessType defaultAccessType) {
		final AnnotationDescription.Loadable<Access> access = annotationSource.getDeclaredAnnotations().ofType( Access.class );
		return access != null ? access.load().value() : defaultAccessType;
	}

	private static boolean hasMappingAnnotation(AnnotationList annotations) {
		return annotations.isAnnotationPresent( Entity.class )
				|| annotations.isAnnotationPresent( MappedSuperclass.class )
				|| annotations.isAnnotationPresent( Embeddable.class );
	}

	private static boolean isPersistentMethod(MethodDescription method) {
		final AnnotationList annotations = method.getDeclaredAnnotations();
		if ( annotations.isAnnotationPresent( Transient.class ) ) {
			return false;
		}

		return annotations.stream().noneMatch( a -> IGNORED_PERSISTENCE_ANNOTATIONS.contains( a.getAnnotationType().getName() ) );
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
			if ( !type.getDeclaredFields().filter( not( isStatic() ).and( named( fieldName ) ) ).isEmpty() ) {
				return true;
			}
			type = type.getSuperClass();
		}
		while ( type != null && !type.represents( Object.class ) );
		return false;
	}

	/**
	 * Check whether an entity class ({@code managedCtClass}) has mismatched names between a persistent field and its
	 * getter/setter when using {@link AccessType#PROPERTY}, which Hibernate does not currently support for enhancement.
	 * See <a href="https://hibernate.atlassian.net/browse/HHH-16572">HHH-16572</a>
	 *
	 * @return {@code true} if enhancement of the class must be {@link UnsupportedEnhancementStrategy#SKIP skipped}
	 * because it has mismatched names.
	 * {@code false} if enhancement of the class must proceed, either because it doesn't have any mismatched names,
	 * or because {@link UnsupportedEnhancementStrategy#LEGACY legacy mode} was opted into.
	 * @throws EnhancementException if enhancement of the class must {@link UnsupportedEnhancementStrategy#FAIL abort} because it has mismatched names.
	 */
	@SuppressWarnings("deprecation")
	private static boolean checkUnsupportedAttributeNaming(TypeDescription managedCtClass, ByteBuddyEnhancementContext enhancementContext) {
		var strategy = enhancementContext.getUnsupportedEnhancementStrategy();
		if ( UnsupportedEnhancementStrategy.LEGACY.equals( strategy ) ) {
			// Don't check anything and act as if there was nothing unsupported in the class.
			// This is unsafe but that's what LEGACY is about.
			return false;
		}

		// For process access rules, See https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2#default-access-type
		// and https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2#a122
		//
		// This check will determine if entity field names do not match Property accessor method name
		// For example:
		// @Entity
		// class Book {
		//   Integer id;
		//   String smtg;
		//
		//   @Id Integer getId() { return id; }
		//   String getSomething() { return smtg; }
		// }
		//
		// Check name of the getter/setter method with persistence annotation and getter/setter method name that doesn't refer to an entity field
		// and will return false.  If the property accessor method(s) are named to match the field name(s), return true.
		final AccessType defaultAccessType = determineDefaultAccessType( managedCtClass );
		final MethodList<?> methods = MethodGraph.Compiler.DEFAULT.compile( (TypeDefinition) managedCtClass )
				.listNodes()
				.asMethodList()
				.filter( isGetter().or( isSetter() ) );
		for ( final MethodDescription methodDescription : methods ) {
			if ( methodDescription.getDeclaringType().represents( Object.class )
				|| determineAccessType( methodDescription, defaultAccessType ) != AccessType.PROPERTY ) {
				// We only need to check this for AccessType.PROPERTY
				continue;
			}

			final String methodName = methodDescription.getActualName();
			String fieldName;
			if ( methodName.startsWith( "get" ) || methodName.startsWith( "set" ) ) {
				fieldName = methodName.substring( 3 );
			}
			else {
				assert methodName.startsWith( "is" );
				fieldName = methodName.substring( 2 );
			}
			// convert first field letter to lower case
			fieldName = getJavaBeansFieldName( fieldName );
			if ( fieldName != null && isPersistentMethod( methodDescription )
				&& !containsField( managedCtClass.asGenericType(), fieldName ) ) {
				// We shouldn't even be in this method if using LEGACY, see top of this method.
				return switch ( strategy ) {
					case SKIP -> {
						log.debugf(
								"Skipping enhancement of [%s] because no field named [%s] could be found for property accessor method [%s]."
								+ " To fix this, make sure all property accessor methods have a matching field.",
								managedCtClass.getName(),
								fieldName,
								methodDescription.getName()
						);
						yield true;
					}
					case FAIL -> throw new EnhancementException( String.format(
							"Enhancement of [%s] failed because no field named [%s] could be found for property accessor method [%s]."
							+ " To fix this, make sure all property accessor methods have a matching field.",
							managedCtClass.getName(),
							fieldName,
							methodDescription.getName()
					) );
					default -> throw new AssertionFailure( "Unexpected strategy at this point: " + strategy );
				};
			}
		}
		return false;
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
		if ( name.length() > 1 && Character.isUpperCase( name.charAt( 1 ) ) && Character.isUpperCase( name.charAt( 0 ) ) ) {
			return name;
		}
		final char[] chars = name.toCharArray();
		chars[0] = Character.toLowerCase( chars[0] );
		return new String( chars );
	}

	private PersistentAttributeTransformer createTransformer(TypeDescription typeDescription) {
		return PersistentAttributeTransformer.collectPersistentFields( typeDescription, enhancementContext, typePool, constants );
	}

	// See HHH-10977 HHH-11284 HHH-11404 --- check for declaration of Managed interface on the class, not inherited
	private boolean alreadyEnhanced(TypeDescription managedCtClass) {
		for ( Generic declaredInterface : managedCtClass.getInterfaces() ) {
			if ( declaredInterface.asErasure().isAssignableTo( Managed.class ) ) {
				return true;
			}
		}
		return false;
	}

	private DynamicType.Builder<?> addInterceptorHandling(DynamicType.Builder<?> builder, TypeDescription managedCtClass) {
		// interceptor handling is only needed if class has lazy-loadable attributes
		if ( enhancementContext.hasLazyLoadableAttributes( managedCtClass ) ) {
			log.tracef( "Weaving in PersistentAttributeInterceptable implementation on [%s]", managedCtClass.getName() );

			builder = builder.implement( constants.INTERFACES_for_PersistentAttributeInterceptable );

			builder = addFieldWithGetterAndSetter(
					builder,
					constants.TypePersistentAttributeInterceptor,
					EnhancerConstants.INTERCEPTOR_FIELD_NAME,
					EnhancerConstants.INTERCEPTOR_GETTER_NAME,
					EnhancerConstants.INTERCEPTOR_SETTER_NAME
			);
		}

		return builder;
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
				.defineMethod( EnhancerConstants.PERSISTENCE_INFO_SETTER_NAME, entityEntryType, constants.modifierPUBLIC )
				// previous, next, instance-id
				.withParameters( entityEntryType, managedEntityType, managedEntityType, intType )
				.intercept( constants.implementationSetPersistenceInfo );
	}

	private List<AnnotatedFieldDescription> collectCollectionFields(TypeDescription managedCtClass) {
		List<AnnotatedFieldDescription> collectionList = new ArrayList<>();

		for ( FieldDescription ctField : managedCtClass.getDeclaredFields() ) {
			// skip static fields and skip fields added by enhancement
			if ( Modifier.isStatic( ctField.getModifiers() ) || ctField.getName().startsWith( "$$_hibernate_" ) ) {
				continue;
			}
			AnnotatedFieldDescription annotatedField = new AnnotatedFieldDescription( enhancementContext, ctField );
			if ( enhancementContext.isPersistentField( annotatedField ) && enhancementContext.isMappedCollection( annotatedField ) ) {
				if ( ctField.getType().asErasure().isAssignableTo( Collection.class ) || ctField.getType().asErasure().isAssignableTo( Map.class ) ) {
					collectionList.add( annotatedField );
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
		TypeDefinition managedCtSuperclass = managedCtClass.getSuperClass();
		if ( managedCtSuperclass == null || managedCtSuperclass.represents( Object.class ) ) {
			return Collections.emptyList();
		}

		if ( !enhancementContext.isMappedSuperclassClass( managedCtSuperclass.asErasure() ) ) {
			return collectInheritCollectionFields( managedCtSuperclass.asErasure() );
		}
		List<AnnotatedFieldDescription> collectionList = new ArrayList<>();

		for ( FieldDescription ctField : managedCtSuperclass.getDeclaredFields() ) {
			if ( !Modifier.isStatic( ctField.getModifiers() ) ) {
				AnnotatedFieldDescription annotatedField = new AnnotatedFieldDescription( enhancementContext, ctField );
				if ( enhancementContext.isPersistentField( annotatedField ) && enhancementContext.isMappedCollection( annotatedField ) ) {
					if ( ctField.getType().asErasure().isAssignableTo( Collection.class ) || ctField.getType().asErasure().isAssignableTo( Map.class ) ) {
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

		boolean isVisibleTo(TypeDescription typeDescription) {
			return fieldDescription.isVisibleTo( typeDescription );
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
			AnnotationDescription.Loadable<Access> access = fieldDescription.getDeclaringType().asErasure()
					.getDeclaredAnnotations().ofType( Access.class );
			if ( access != null && access.load().value() == AccessType.PROPERTY ) {
				Optional<MethodDescription> getter = getGetter();
				if ( getter.isPresent() ) {
					return getter.get().getDeclaredAnnotations();
				}
				else {
					return fieldDescription.getDeclaredAnnotations();
				}
			}
			else if ( access != null && access.load().value() == AccessType.FIELD ) {
				return fieldDescription.getDeclaredAnnotations();
			}
			else {
				Optional<MethodDescription> getter = getGetter();

				// Note that the order here is important
				List<AnnotationDescription> annotationDescriptions = new ArrayList<>();
				if ( getter.isPresent() ) {
					annotationDescriptions.addAll( getter.get().getDeclaredAnnotations() );
				}
				annotationDescriptions.addAll( fieldDescription.getDeclaredAnnotations() );

				return new AnnotationList.Explicit( annotationDescriptions );
			}
		}
	}


	private static EnhancementInfo createInfoAnnotation(EnhancementContext enhancementContext) {
		return new EnhancementInfoImpl( enhancementContext.doDirtyCheckingInline(), enhancementContext.doBiDirectionalAssociationManagement() );
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
