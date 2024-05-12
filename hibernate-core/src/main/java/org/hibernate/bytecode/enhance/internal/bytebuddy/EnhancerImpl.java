/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.hibernate.Version;
import org.hibernate.bytecode.enhance.VersionMismatchException;
import org.hibernate.bytecode.enhance.internal.tracker.CompositeOwnerTracker;
import org.hibernate.bytecode.enhance.internal.tracker.DirtyTracker;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.EnhancementInfo;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.bytecode.enhance.spi.UnloadedField;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;
import org.hibernate.engine.spi.CompositeOwner;
import org.hibernate.engine.spi.CompositeTracker;
import org.hibernate.engine.spi.ExtendedSelfDirtinessTracker;
import org.hibernate.engine.spi.Managed;
import org.hibernate.engine.spi.ManagedComposite;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.ManagedMappedSuperclass;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.metamodel.Type;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldDescription.InDefinedShape;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.StubMethod;

import static net.bytebuddy.matcher.ElementMatchers.isDefaultFinalizer;

public class EnhancerImpl implements Enhancer {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( Enhancer.class );

	protected final ByteBuddyEnhancementContext enhancementContext;
	private final ByteBuddyState byteBuddyState;
	private final EnhancerClassLocator typePool;
	private final EnhancerImplConstants constants;

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
		this.enhancementContext = new ByteBuddyEnhancementContext( enhancementContext );
		this.byteBuddyState = Objects.requireNonNull( byteBuddyState );
		this.typePool = Objects.requireNonNull( classLocator );
		this.constants = byteBuddyState.getEnhancerConstants();
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
					() -> byteBuddy.ignore( isDefaultFinalizer() )
							.redefine( typeDescription, typePool.asClassFileLocator() )
							.annotateType( constants.HIBERNATE_VERSION_ANNOTATION ),
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
		// can't effectively enhance interfaces
		if ( managedCtClass.isInterface() ) {
			log.debugf( "Skipping enhancement of [%s]: it's an interface", managedCtClass.getName() );
			return null;
		}

		// can't effectively enhance records
		if ( managedCtClass.isRecord() ) {
			log.debugf( "Skipping enhancement of [%s]: it's a record", managedCtClass.getName() );
			return null;
		}

		// handle already enhanced classes
		if ( alreadyEnhanced( managedCtClass ) ) {
			verifyVersions( managedCtClass, enhancementContext );

			log.debugf( "Skipping enhancement of [%s]: already enhanced", managedCtClass.getName() );
			return null;
		}

		if ( enhancementContext.isEntityClass( managedCtClass ) ) {
			log.debugf( "Enhancing [%s] as Entity", managedCtClass.getName() );
			DynamicType.Builder<?> builder = builderSupplier.get();
			builder = builder.implement( ManagedEntity.class )
					.defineMethod( EnhancerConstants.ENTITY_INSTANCE_GETTER_NAME, constants.TypeObject, constants.methodModifierPUBLIC )
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

			builder = addInterceptorHandling( builder, managedCtClass );

			if ( enhancementContext.doDirtyCheckingInline( managedCtClass ) ) {
				List<AnnotatedFieldDescription> collectionFields = collectCollectionFields( managedCtClass );

				if ( collectionFields.isEmpty() ) {
					builder = builder.implement( SelfDirtinessTracker.class )
							.defineField( EnhancerConstants.TRACKER_FIELD_NAME, DirtyTracker.class, constants.fieldModifierPRIVATE_TRANSIENT )
									.annotateField( constants.TRANSIENT_ANNOTATION )
							.defineMethod( EnhancerConstants.TRACKER_CHANGER_NAME, constants.TypeVoid, constants.methodModifierPUBLIC )
									.withParameters( String.class )
									.intercept( constants.implementationTrackChange )
							.defineMethod( EnhancerConstants.TRACKER_GET_NAME, constants.Type_Array_String, constants.methodModifierPUBLIC )
									.intercept( constants.implementationGetDirtyAttributesWithoutCollections )
							.defineMethod( EnhancerConstants.TRACKER_HAS_CHANGED_NAME, constants.TypeBooleanPrimitive, constants.methodModifierPUBLIC )
									.intercept( constants.implementationAreFieldsDirtyWithoutCollections )
							.defineMethod( EnhancerConstants.TRACKER_CLEAR_NAME, constants.TypeVoid, constants.methodModifierPUBLIC )
									.intercept( constants.implementationClearDirtyAttributesWithoutCollections )
							.defineMethod( EnhancerConstants.TRACKER_SUSPEND_NAME, constants.TypeVoid, constants.methodModifierPUBLIC )
									.withParameters( constants.TypeBooleanPrimitive )
									.intercept( constants.implementationSuspendDirtyTracking )
							.defineMethod( EnhancerConstants.TRACKER_COLLECTION_GET_NAME, constants.TypeCollectionTracker, constants.methodModifierPUBLIC )
									.intercept( constants.implementationGetCollectionTrackerWithoutCollections );
				}
				else {
					//TODO es.enableInterfaceExtendedSelfDirtinessTracker ? Careful with consequences..
					builder = builder.implement( ExtendedSelfDirtinessTracker.class )
							.defineField( EnhancerConstants.TRACKER_FIELD_NAME, DirtyTracker.class, constants.fieldModifierPRIVATE_TRANSIENT )
									.annotateField( constants.TRANSIENT_ANNOTATION )
							.defineField( EnhancerConstants.TRACKER_COLLECTION_NAME, constants.TypeCollectionTracker, constants.fieldModifierPRIVATE_TRANSIENT )
									.annotateField( constants.TRANSIENT_ANNOTATION )
							.defineMethod( EnhancerConstants.TRACKER_CHANGER_NAME, constants.TypeVoid, constants.methodModifierPUBLIC )
									.withParameters( String.class )
									.intercept( constants.implementationTrackChange )
							.defineMethod( EnhancerConstants.TRACKER_GET_NAME, constants.Type_Array_String, constants.methodModifierPUBLIC )
									.intercept( constants.implementationGetDirtyAttributes )
							.defineMethod( EnhancerConstants.TRACKER_HAS_CHANGED_NAME, constants.TypeBooleanPrimitive, constants.methodModifierPUBLIC )
									.intercept( constants.implementationAreFieldsDirty )
							.defineMethod( EnhancerConstants.TRACKER_CLEAR_NAME, constants.TypeVoid, constants.methodModifierPUBLIC )
									.intercept( constants.implementationClearDirtyAttributes )
							.defineMethod( EnhancerConstants.TRACKER_SUSPEND_NAME, constants.TypeVoid, constants.methodModifierPUBLIC )
									.withParameters( constants.TypeBooleanPrimitive )
									.intercept( constants.implementationSuspendDirtyTracking )
							.defineMethod( EnhancerConstants.TRACKER_COLLECTION_GET_NAME, constants.TypeCollectionTracker, constants.methodModifierPUBLIC )
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

					builder = builder.defineMethod( EnhancerConstants.TRACKER_COLLECTION_CHANGED_NAME, constants.TypeBooleanPrimitive, constants.methodModifierPUBLIC )
							.intercept( isDirty )
							.defineMethod( EnhancerConstants.TRACKER_COLLECTION_CHANGED_FIELD_NAME, constants.TypeVoid, constants.methodModifierPUBLIC )
									.withParameters( DirtyTracker.class )
									.intercept( getDirtyNames )
							.defineMethod( EnhancerConstants.TRACKER_COLLECTION_CLEAR_NAME, constants.TypeVoid, constants.methodModifierPUBLIC )
									.intercept( Advice.withCustomMapping()
									.to( CodeTemplates.ClearDirtyCollectionNames.class, constants.adviceLocator )
									.wrap( StubMethod.INSTANCE ) )
							.defineMethod( ExtendedSelfDirtinessTracker.REMOVE_DIRTY_FIELDS_NAME, constants.TypeVoid, constants.methodModifierPUBLIC )
									.withParameters( LazyAttributeLoadingInterceptor.class )
									.intercept( clearDirtyNames );
				}
			}

			return createTransformer( managedCtClass ).applyTo( builder );
		}
		else if ( enhancementContext.isCompositeClass( managedCtClass ) ) {
			log.debugf( "Enhancing [%s] as Composite", managedCtClass.getName() );

			DynamicType.Builder<?> builder = builderSupplier.get();
			builder = builder.implement( ManagedComposite.class );
			builder = addInterceptorHandling( builder, managedCtClass );

			if ( enhancementContext.doDirtyCheckingInline( managedCtClass ) ) {
				builder = builder.implement( CompositeTracker.class )
						.defineField(
								EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME,
								CompositeOwnerTracker.class,
								constants.fieldModifierPRIVATE_TRANSIENT
						)
								.annotateField( constants.TRANSIENT_ANNOTATION )
						.defineMethod(
								EnhancerConstants.TRACKER_COMPOSITE_SET_OWNER,
								constants.TypeVoid,
								constants.methodModifierPUBLIC
						)
								.withParameters( String.class, CompositeOwner.class )
								.intercept( constants.implementationSetOwner )
						.defineMethod(
								EnhancerConstants.TRACKER_COMPOSITE_CLEAR_OWNER,
								constants.TypeVoid,
								constants.methodModifierPUBLIC
						)
								.withParameters( String.class )
								.intercept( constants.implementationClearOwner );
			}

			return createTransformer( managedCtClass ).applyTo( builder );
		}
		else if ( enhancementContext.isMappedSuperclassClass( managedCtClass ) ) {
			log.debugf( "Enhancing [%s] as MappedSuperclass", managedCtClass.getName() );

			DynamicType.Builder<?> builder = builderSupplier.get();
			builder = builder.implement( ManagedMappedSuperclass.class );
			return createTransformer( managedCtClass ).applyTo( builder );
		}
		else if ( enhancementContext.doExtendedEnhancement( managedCtClass ) ) {
			log.debugf( "Extended enhancement of [%s]", managedCtClass.getName() );
			return createTransformer( managedCtClass ).applyExtended( builderSupplier.get() );
		}
		else {
			log.debugf( "Skipping enhancement of [%s]: not entity or composite", managedCtClass.getName() );
			return null;
		}
	}

	private static void verifyVersions(TypeDescription managedCtClass, ByteBuddyEnhancementContext enhancementContext) {
		final AnnotationDescription.Loadable<EnhancementInfo> existingInfo = managedCtClass
				.getDeclaredAnnotations()
				.ofType( EnhancementInfo.class );
		if ( existingInfo == null ) {
			// There is an edge case here where a user manually adds `implement Managed` to
			// their domain class, in which case there will most likely not be a
			// `EnhancementInfo` annotation.  Such cases should simply not do version checking.
			//
			// However, there is also ambiguity in this case with classes that were enhanced
			// with old versions of Hibernate which did not add that annotation as part of
			// enhancement.  But overall we consider this condition to be acceptable
			return;
		}

		final String enhancementVersion = extractVersion( existingInfo );
		if ( !Version.getVersionString().equals( enhancementVersion ) ) {
			throw new VersionMismatchException( managedCtClass, enhancementVersion, Version.getVersionString() );
		}
	}

	private static String extractVersion(AnnotationDescription.Loadable<EnhancementInfo> annotation) {
		return annotation.load().version();
	}

	private PersistentAttributeTransformer createTransformer(TypeDescription typeDescription) {
		return PersistentAttributeTransformer.collectPersistentFields( typeDescription, enhancementContext, typePool );
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
			log.debugf( "Weaving in PersistentAttributeInterceptable implementation on [%s]", managedCtClass.getName() );

			builder = builder.implement( PersistentAttributeInterceptable.class );

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
				.defineField( fieldName, type, constants.fieldModifierPRIVATE_TRANSIENT )
						.annotateField( constants.TRANSIENT_ANNOTATION )
				.defineMethod( getterName, type, constants.methodModifierPUBLIC )
						.intercept( FieldAccessor.ofField( fieldName ) )
				.defineMethod( setterName, constants.TypeVoid, constants.methodModifierPUBLIC )
						.withParameters( type )
						.intercept( FieldAccessor.ofField( fieldName ) );
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

}
