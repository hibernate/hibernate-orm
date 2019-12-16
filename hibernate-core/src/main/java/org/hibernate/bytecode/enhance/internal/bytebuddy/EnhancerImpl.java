/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import static net.bytebuddy.matcher.ElementMatchers.isDefaultFinalizer;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Transient;

import org.hibernate.bytecode.enhance.internal.tracker.CompositeOwnerTracker;
import org.hibernate.bytecode.enhance.internal.tracker.DirtyTracker;
import org.hibernate.bytecode.enhance.spi.CollectionTracker;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.bytecode.enhance.spi.UnloadedField;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;
import org.hibernate.engine.spi.CompositeOwner;
import org.hibernate.engine.spi.CompositeTracker;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ExtendedSelfDirtinessTracker;
import org.hibernate.engine.spi.Managed;
import org.hibernate.engine.spi.ManagedComposite;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.ManagedMappedSuperclass;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldDescription.InDefinedShape;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.FieldPersistence;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.pool.TypePool;

public class EnhancerImpl implements Enhancer {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( Enhancer.class );

	protected final ByteBuddyEnhancementContext enhancementContext;
	private final ByteBuddyState byteBuddyState;

	private final EnhancerClassFileLocator classFileLocator;
	private final TypePool typePool;

	/**
	 * Extract the following constants so that enhancement on large projects
	 * can be done efficiently: otherwise each instance use will trigger a
	 * resource load on the ClassLoader tree, triggering allocation of
	 * several streams to unzip each JAR file each time.
	 */
	private final ClassFileLocator adviceLocator = ClassFileLocator.ForClassLoader.of(CodeTemplates.class.getClassLoader());
	private final Implementation implementationTrackChange = Advice.to( CodeTemplates.TrackChange.class, adviceLocator ).wrap( StubMethod.INSTANCE );
	private final Implementation implementationGetDirtyAttributesWithoutCollections = Advice.to( CodeTemplates.GetDirtyAttributesWithoutCollections.class, adviceLocator ).wrap( StubMethod.INSTANCE );
	private final Implementation implementationAreFieldsDirtyWithoutCollections = Advice.to( CodeTemplates.AreFieldsDirtyWithoutCollections.class, adviceLocator ).wrap( StubMethod.INSTANCE );
	private final Implementation implementationClearDirtyAttributesWithoutCollections = Advice.to( CodeTemplates.ClearDirtyAttributesWithoutCollections.class, adviceLocator ).wrap( StubMethod.INSTANCE );
	private final Implementation implementationSuspendDirtyTracking = Advice.to( CodeTemplates.SuspendDirtyTracking.class, adviceLocator ).wrap( StubMethod.INSTANCE );
	private final Implementation implementationGetDirtyAttributes = Advice.to( CodeTemplates.GetDirtyAttributes.class, adviceLocator ).wrap( StubMethod.INSTANCE );
	private final Implementation implementationAreFieldsDirty = Advice.to( CodeTemplates.AreFieldsDirty.class, adviceLocator ).wrap( StubMethod.INSTANCE );
	private final Implementation implementationGetCollectionTrackerWithoutCollections = Advice.to( CodeTemplates.GetCollectionTrackerWithoutCollections.class, adviceLocator ).wrap( StubMethod.INSTANCE );
	private final Implementation implementationClearDirtyAttributes = Advice.to( CodeTemplates.ClearDirtyAttributes.class, adviceLocator ).wrap( StubMethod.INSTANCE );
	//In this case we just extract the Advice:
	private final Advice adviceInitializeLazyAttributeLoadingInterceptor = Advice.to( CodeTemplates.InitializeLazyAttributeLoadingInterceptor.class, adviceLocator );
	private final Implementation implementationSetOwner = Advice.to( CodeTemplates.SetOwner.class, adviceLocator ).wrap( StubMethod.INSTANCE );
	private final Implementation implementationClearOwner = Advice.to( CodeTemplates.ClearOwner.class, adviceLocator ).wrap( StubMethod.INSTANCE );

	/**
	 * Constructs the Enhancer, using the given context.
	 *
	 * @param enhancementContext Describes the context in which enhancement will occur so as to give access
	 * to contextual/environmental information.
	 * @param byteBuddyState refers to the ByteBuddy instance to use
	 */
	public EnhancerImpl(final EnhancementContext enhancementContext, final ByteBuddyState byteBuddyState) {
		this.enhancementContext = new ByteBuddyEnhancementContext( enhancementContext );
		this.byteBuddyState = byteBuddyState;
		this.classFileLocator = new EnhancerClassFileLocator( enhancementContext.getLoadingClassLoader() );
		this.typePool = buildTypePool( classFileLocator );
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
		classFileLocator.setClassNameAndBytes( safeClassName, originalBytes );
		try {
			final TypeDescription typeDescription = typePool.describe( safeClassName ).resolve();

			return byteBuddyState.rewrite( typePool, safeClassName, byteBuddy -> doEnhance(
					byteBuddy.ignore( isDefaultFinalizer() ).redefine( typeDescription, ClassFileLocator.Simple.of( safeClassName, originalBytes ) ),
					typeDescription
			) );
		}
		catch (RuntimeException e) {
			throw new EnhancementException( "Failed to enhance class " + className, e );
		}
	}

	private TypePool buildTypePool(final ClassFileLocator classFileLocator) {
		return TypePool.Default.WithLazyResolution.of( classFileLocator );
	}

	private DynamicType.Builder<?> doEnhance(DynamicType.Builder<?> builder, TypeDescription managedCtClass) {
		// can't effectively enhance interfaces
		if ( managedCtClass.isInterface() ) {
			log.debugf( "Skipping enhancement of [%s]: it's an interface!", managedCtClass.getName() );
			return null;
		}
		// skip already enhanced classes
		if ( alreadyEnhanced( managedCtClass ) ) {
			log.debugf( "Skipping enhancement of [%s]: already enhanced", managedCtClass.getName() );
			return null;
		}

		if ( enhancementContext.isEntityClass( managedCtClass ) ) {
			log.debugf( "Enhancing [%s] as Entity", managedCtClass.getName() );
			builder = builder.implement( ManagedEntity.class )
					.defineMethod( EnhancerConstants.ENTITY_INSTANCE_GETTER_NAME, Object.class, Visibility.PUBLIC )
					.intercept( FixedValue.self() );

			builder = addFieldWithGetterAndSetter(
					builder,
					EntityEntry.class,
					EnhancerConstants.ENTITY_ENTRY_FIELD_NAME,
					EnhancerConstants.ENTITY_ENTRY_GETTER_NAME,
					EnhancerConstants.ENTITY_ENTRY_SETTER_NAME
			);
			builder = addFieldWithGetterAndSetter(
					builder,
					ManagedEntity.class,
					EnhancerConstants.PREVIOUS_FIELD_NAME,
					EnhancerConstants.PREVIOUS_GETTER_NAME,
					EnhancerConstants.PREVIOUS_SETTER_NAME
			);
			builder = addFieldWithGetterAndSetter(
					builder,
					ManagedEntity.class,
					EnhancerConstants.NEXT_FIELD_NAME,
					EnhancerConstants.NEXT_GETTER_NAME,
					EnhancerConstants.NEXT_SETTER_NAME
			);

			builder = addInterceptorHandling( builder, managedCtClass );

			if ( enhancementContext.doDirtyCheckingInline( managedCtClass ) ) {
				List<AnnotatedFieldDescription> collectionFields = collectCollectionFields( managedCtClass );

				if ( collectionFields.isEmpty() ) {
					builder = builder.implement( SelfDirtinessTracker.class )
							.defineField( EnhancerConstants.TRACKER_FIELD_NAME, DirtyTracker.class, FieldPersistence.TRANSIENT, Visibility.PRIVATE )
									.annotateField( AnnotationDescription.Builder.ofType( Transient.class ).build() )
							.defineMethod( EnhancerConstants.TRACKER_CHANGER_NAME, void.class, Visibility.PUBLIC )
									.withParameters( String.class )
									.intercept( implementationTrackChange )
							.defineMethod( EnhancerConstants.TRACKER_GET_NAME, String[].class, Visibility.PUBLIC )
									.intercept( implementationGetDirtyAttributesWithoutCollections )
							.defineMethod( EnhancerConstants.TRACKER_HAS_CHANGED_NAME, boolean.class, Visibility.PUBLIC )
									.intercept( implementationAreFieldsDirtyWithoutCollections )
							.defineMethod( EnhancerConstants.TRACKER_CLEAR_NAME, void.class, Visibility.PUBLIC )
									.intercept( implementationClearDirtyAttributesWithoutCollections )
							.defineMethod( EnhancerConstants.TRACKER_SUSPEND_NAME, void.class, Visibility.PUBLIC )
									.withParameters( boolean.class )
									.intercept( implementationSuspendDirtyTracking )
							.defineMethod( EnhancerConstants.TRACKER_COLLECTION_GET_NAME, CollectionTracker.class, Visibility.PUBLIC )
									.intercept( implementationGetCollectionTrackerWithoutCollections );
				}
				else {
					builder = builder.implement( ExtendedSelfDirtinessTracker.class )
							.defineField( EnhancerConstants.TRACKER_FIELD_NAME, DirtyTracker.class, FieldPersistence.TRANSIENT, Visibility.PRIVATE )
									.annotateField( AnnotationDescription.Builder.ofType( Transient.class ).build() )
							.defineField( EnhancerConstants.TRACKER_COLLECTION_NAME, CollectionTracker.class, FieldPersistence.TRANSIENT, Visibility.PRIVATE )
									.annotateField( AnnotationDescription.Builder.ofType( Transient.class ).build() )
							.defineMethod( EnhancerConstants.TRACKER_CHANGER_NAME, void.class, Visibility.PUBLIC )
									.withParameters( String.class )
									.intercept( implementationTrackChange )
							.defineMethod( EnhancerConstants.TRACKER_GET_NAME, String[].class, Visibility.PUBLIC )
									.intercept( implementationGetDirtyAttributes )
							.defineMethod( EnhancerConstants.TRACKER_HAS_CHANGED_NAME, boolean.class, Visibility.PUBLIC )
									.intercept( implementationAreFieldsDirty )
							.defineMethod( EnhancerConstants.TRACKER_CLEAR_NAME, void.class, Visibility.PUBLIC )
									.intercept( implementationClearDirtyAttributes )
							.defineMethod( EnhancerConstants.TRACKER_SUSPEND_NAME, void.class, Visibility.PUBLIC )
									.withParameters( boolean.class )
									.intercept( implementationSuspendDirtyTracking )
							.defineMethod( EnhancerConstants.TRACKER_COLLECTION_GET_NAME, CollectionTracker.class, Visibility.PUBLIC )
									.intercept( FieldAccessor.ofField( EnhancerConstants.TRACKER_COLLECTION_NAME ) );

					Implementation isDirty = StubMethod.INSTANCE, getDirtyNames = StubMethod.INSTANCE, clearDirtyNames = StubMethod.INSTANCE;
					for ( AnnotatedFieldDescription collectionField : collectionFields ) {
						if ( collectionField.getType().asErasure().isAssignableTo( Map.class ) ) {
							isDirty = Advice.withCustomMapping()
									.bind( CodeTemplates.FieldName.class, collectionField.getName() )
									.bind( CodeTemplates.FieldValue.class, collectionField.getFieldDescription() )
									.to( CodeTemplates.MapAreCollectionFieldsDirty.class, adviceLocator )
									.wrap( isDirty );
							getDirtyNames = Advice.withCustomMapping()
									.bind( CodeTemplates.FieldName.class, collectionField.getName() )
									.bind( CodeTemplates.FieldValue.class, collectionField.getFieldDescription() )
									.to( CodeTemplates.MapGetCollectionFieldDirtyNames.class, adviceLocator )
									.wrap( getDirtyNames );
							clearDirtyNames = Advice.withCustomMapping()
									.bind( CodeTemplates.FieldName.class, collectionField.getName() )
									.bind( CodeTemplates.FieldValue.class, collectionField.getFieldDescription() )
									.to( CodeTemplates.MapGetCollectionClearDirtyNames.class, adviceLocator )
									.wrap( clearDirtyNames );
						}
						else {
							isDirty = Advice.withCustomMapping()
									.bind( CodeTemplates.FieldName.class, collectionField.getName() )
									.bind( CodeTemplates.FieldValue.class, collectionField.getFieldDescription() )
									.to( CodeTemplates.CollectionAreCollectionFieldsDirty.class, adviceLocator )
									.wrap( isDirty );
							getDirtyNames = Advice.withCustomMapping()
									.bind( CodeTemplates.FieldName.class, collectionField.getName() )
									.bind( CodeTemplates.FieldValue.class, collectionField.getFieldDescription() )
									.to( CodeTemplates.CollectionGetCollectionFieldDirtyNames.class, adviceLocator )
									.wrap( getDirtyNames );
							clearDirtyNames = Advice.withCustomMapping()
									.bind( CodeTemplates.FieldName.class, collectionField.getName() )
									.bind( CodeTemplates.FieldValue.class, collectionField.getFieldDescription() )
									.to( CodeTemplates.CollectionGetCollectionClearDirtyNames.class, adviceLocator )
									.wrap( clearDirtyNames );
						}
					}

					if ( enhancementContext.hasLazyLoadableAttributes( managedCtClass ) ) {
						clearDirtyNames = adviceInitializeLazyAttributeLoadingInterceptor.wrap( clearDirtyNames );
					}

					builder = builder.defineMethod( EnhancerConstants.TRACKER_COLLECTION_CHANGED_NAME, boolean.class, Visibility.PUBLIC )
							.intercept( isDirty )
							.defineMethod( EnhancerConstants.TRACKER_COLLECTION_CHANGED_FIELD_NAME, void.class, Visibility.PUBLIC )
									.withParameters( DirtyTracker.class )
									.intercept( getDirtyNames )
							.defineMethod( EnhancerConstants.TRACKER_COLLECTION_CLEAR_NAME, void.class, Visibility.PUBLIC )
									.intercept( Advice.withCustomMapping()
									.to( CodeTemplates.ClearDirtyCollectionNames.class, adviceLocator )
									.wrap( StubMethod.INSTANCE ) )
							.defineMethod( ExtendedSelfDirtinessTracker.REMOVE_DIRTY_FIELDS_NAME, void.class, Visibility.PUBLIC )
									.withParameters( LazyAttributeLoadingInterceptor.class )
									.intercept( clearDirtyNames );
				}
			}

			return createTransformer( managedCtClass ).applyTo( builder );
		}
		else if ( enhancementContext.isCompositeClass( managedCtClass ) ) {
			log.debugf( "Enhancing [%s] as Composite", managedCtClass.getName() );

			builder = builder.implement( ManagedComposite.class );
			builder = addInterceptorHandling( builder, managedCtClass );

			if ( enhancementContext.doDirtyCheckingInline( managedCtClass ) ) {
				builder = builder.implement( CompositeTracker.class )
						.defineField(
								EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME,
								CompositeOwnerTracker.class,
								FieldPersistence.TRANSIENT,
								Visibility.PRIVATE
						)
								.annotateField( AnnotationDescription.Builder.ofType( Transient.class ).build() )
						.defineMethod(
								EnhancerConstants.TRACKER_COMPOSITE_SET_OWNER,
								void.class,
								Visibility.PUBLIC
						)
								.withParameters( String.class, CompositeOwner.class )
								.intercept( implementationSetOwner )
						.defineMethod(
								EnhancerConstants.TRACKER_COMPOSITE_CLEAR_OWNER,
								void.class,
								Visibility.PUBLIC
						)
								.withParameters( String.class )
								.intercept( implementationClearOwner );
			}

			return createTransformer( managedCtClass ).applyTo( builder );
		}
		else if ( enhancementContext.isMappedSuperclassClass( managedCtClass ) ) {
			log.debugf( "Enhancing [%s] as MappedSuperclass", managedCtClass.getName() );

			builder = builder.implement( ManagedMappedSuperclass.class );
			return createTransformer( managedCtClass ).applyTo( builder );
		}
		else if ( enhancementContext.doExtendedEnhancement( managedCtClass ) ) {
			log.debugf( "Extended enhancement of [%s]", managedCtClass.getName() );
			return createTransformer( managedCtClass ).applyExtended( builder );
		}
		else {
			log.debugf( "Skipping enhancement of [%s]: not entity or composite", managedCtClass.getName() );
			return null;
		}
	}

	private PersistentAttributeTransformer createTransformer(TypeDescription typeDescription) {
		return PersistentAttributeTransformer.collectPersistentFields( typeDescription, enhancementContext, typePool );
	}

	// See HHH-10977 HHH-11284 HHH-11404 --- check for declaration of Managed interface on the class, not inherited
	private boolean alreadyEnhanced(TypeDescription managedCtClass) {
		for ( TypeDescription.Generic declaredInterface : managedCtClass.getInterfaces() ) {
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
					PersistentAttributeInterceptor.class,
					EnhancerConstants.INTERCEPTOR_FIELD_NAME,
					EnhancerConstants.INTERCEPTOR_GETTER_NAME,
					EnhancerConstants.INTERCEPTOR_SETTER_NAME
			);
		}

		return builder;
	}

	private static DynamicType.Builder<?> addFieldWithGetterAndSetter(
			DynamicType.Builder<?> builder,
			Class<?> type,
			String fieldName,
			String getterName,
			String setterName) {
		return builder
				.defineField( fieldName, type, Visibility.PRIVATE, FieldPersistence.TRANSIENT )
						.annotateField( AnnotationDescription.Builder.ofType( Transient.class ).build() )
				.defineMethod( getterName, type, Visibility.PUBLIC )
						.intercept( FieldAccessor.ofField( fieldName ) )
				.defineMethod( setterName, void.class, Visibility.PUBLIC )
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
			if ( enhancementContext.isPersistentField( annotatedField ) && !enhancementContext.isMappedCollection( annotatedField ) ) {
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
				if ( enhancementContext.isPersistentField( annotatedField ) && !enhancementContext.isMappedCollection( annotatedField ) ) {
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

	private static class EnhancerClassFileLocator extends ClassFileLocator.ForClassLoader {

		// The name of the class to (possibly be) transformed.
		private String className;
		// The explicitly resolved Resolution for the class to (possibly be) transformed.
		private Resolution resolution;

		/**
		 * Creates a new class file locator for the given class loader.
		 *
		 * @param classLoader The class loader to query which must not be the bootstrap class loader, i.e. {@code null}.
		 */
		protected EnhancerClassFileLocator(ClassLoader classLoader) {
			super( classLoader );
		}

		@Override
		public Resolution locate(String className) throws IOException {
			assert className != null;
			if ( className.equals( this.className ) ) {
				return resolution;
			}
			else {
				return super.locate( className );
			}
		}

		void setClassNameAndBytes(String className, byte[] bytes) {
			assert className != null;
			assert bytes != null;
			this.className = className;
			this.resolution = new Resolution.Explicit( bytes);
		}
	}
}
