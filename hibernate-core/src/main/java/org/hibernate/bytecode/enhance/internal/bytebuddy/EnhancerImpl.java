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
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.engine.spi.ExtendedSelfDirtinessTracker;
import org.hibernate.engine.spi.CompositeOwner;
import org.hibernate.engine.spi.CompositeTracker;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.Managed;
import org.hibernate.engine.spi.ManagedComposite;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.ManagedMappedSuperclass;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.pool.TypePool;

import static net.bytebuddy.matcher.ElementMatchers.isGetter;

public class EnhancerImpl implements Enhancer {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( Enhancer.class );

	protected final ByteBuddyEnhancementContext enhancementContext;

	private final TypePool classPool;

	/**
	 * Constructs the Enhancer, using the given context.
	 *
	 * @param enhancementContext Describes the context in which enhancement will occur so as to give access
	 * to contextual/environmental information.
	 */
	public EnhancerImpl(EnhancementContext enhancementContext) {
		this.enhancementContext = new ByteBuddyEnhancementContext( enhancementContext );
		classPool = buildClassPool( this.enhancementContext );
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
	public synchronized byte[] enhance(String className, byte[] originalBytes) throws EnhancementException {
		try {
			final TypeDescription managedCtClass = classPool.describe( className ).resolve();
			DynamicType.Builder<?> builder = doEnhance(
					new ByteBuddy().with( TypeValidation.DISABLED ).redefine( managedCtClass, ClassFileLocator.Simple.of( className, originalBytes ) ),
					managedCtClass
			);
			if ( builder == null ) {
				return null;
			}
			else {
				return builder.make().getBytes();
			}
		}
		catch (RuntimeException e) {
			e.printStackTrace();
			log.unableToBuildEnhancementMetamodel( className );
			return null;
		}
	}

	private TypePool buildClassPool(final ByteBuddyEnhancementContext enhancementContext) {
		return TypePool.Default.WithLazyResolution.of( enhancementContext.getLoadingClassLoader() );
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

		PersistentAttributeTransformer transformer = PersistentAttributeTransformer.collectPersistentFields( managedCtClass, enhancementContext, classPool );

		if ( enhancementContext.isEntityClass( managedCtClass ) ) {
			log.infof( "Enhancing [%s] as Entity", managedCtClass.getName() );
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
				builder = builder.implement( ExtendedSelfDirtinessTracker.class )
						.defineField( EnhancerConstants.TRACKER_FIELD_NAME, DirtyTracker.class, FieldManifestation.TRANSIENT, Visibility.PRIVATE )
						.annotateField( AnnotationDescription.Builder.ofType( Transient.class ).build() )
						.defineField( EnhancerConstants.TRACKER_COLLECTION_NAME, CollectionTracker.class, FieldManifestation.TRANSIENT, Visibility.PRIVATE )
						.annotateField( AnnotationDescription.Builder.ofType( Transient.class ).build() )
						.defineMethod( EnhancerConstants.TRACKER_CHANGER_NAME, void.class, Visibility.PUBLIC )
						.withParameters( String.class )
						.intercept( Advice.to( CodeTemplates.TrackChange.class ).wrap( StubMethod.INSTANCE ) )
						.defineMethod( EnhancerConstants.TRACKER_GET_NAME, String[].class, Visibility.PUBLIC )
						.intercept( Advice.to( CodeTemplates.GetDirtyAttributes.class ).wrap( StubMethod.INSTANCE ) )
						.defineMethod( EnhancerConstants.TRACKER_HAS_CHANGED_NAME, boolean.class, Visibility.PUBLIC )
						.intercept( Advice.to( CodeTemplates.AreCollectionFieldsDirty.class ).wrap( StubMethod.INSTANCE ) )
						.defineMethod( EnhancerConstants.TRACKER_CLEAR_NAME, void.class, Visibility.PUBLIC )
						.intercept( Advice.to( CodeTemplates.ClearDirtyAttributes.class ).wrap( StubMethod.INSTANCE ) )
						.defineMethod( EnhancerConstants.TRACKER_SUSPEND_NAME, void.class, Visibility.PUBLIC )
						.withParameters( boolean.class )
						.intercept( Advice.to( CodeTemplates.SuspendDirtyTracking.class ).wrap( StubMethod.INSTANCE ) )
						.defineMethod( EnhancerConstants.TRACKER_COLLECTION_GET_NAME, CollectionTracker.class, Visibility.PUBLIC )
						.intercept( FieldAccessor.ofField( EnhancerConstants.TRACKER_COLLECTION_NAME ) );

				Implementation isDirty = StubMethod.INSTANCE, getDirtyNames = StubMethod.INSTANCE, clearDirtyNames = StubMethod.INSTANCE;
				for ( FieldDescription collectionField : collectCollectionFields( managedCtClass ) ) {
					if ( !enhancementContext.isMappedCollection( collectionField ) ) {
						if ( collectionField.getType().asErasure().isAssignableTo( Map.class ) ) {
							isDirty = Advice.withCustomMapping()
									.bind( CodeTemplates.FieldName.class, collectionField.getName() )
									.bind( CodeTemplates.FieldValue.class, collectionField )
									.to( CodeTemplates.MapAreCollectionFieldsDirty.class )
									.wrap( isDirty );
							getDirtyNames = Advice.withCustomMapping()
									.bind( CodeTemplates.FieldName.class, collectionField.getName() )
									.bind( CodeTemplates.FieldValue.class, collectionField )
									.to( CodeTemplates.MapGetCollectionFieldDirtyNames.class )
									.wrap( getDirtyNames );
							clearDirtyNames = Advice.withCustomMapping()
									.bind( CodeTemplates.FieldName.class, collectionField.getName() )
									.bind( CodeTemplates.FieldValue.class, collectionField )
									.to( CodeTemplates.MapGetCollectionClearDirtyNames.class )
									.wrap( clearDirtyNames );
						}
						else {
							isDirty = Advice.withCustomMapping()
									.bind( CodeTemplates.FieldName.class, collectionField.getName() )
									.bind( CodeTemplates.FieldValue.class, collectionField )
									.to( CodeTemplates.CollectionAreCollectionFieldsDirty.class )
									.wrap( isDirty );
							getDirtyNames = Advice.withCustomMapping()
									.bind( CodeTemplates.FieldName.class, collectionField.getName() )
									.bind( CodeTemplates.FieldValue.class, collectionField )
									.to( CodeTemplates.CollectionGetCollectionFieldDirtyNames.class )
									.wrap( getDirtyNames );
							clearDirtyNames = Advice.withCustomMapping()
									.bind( CodeTemplates.FieldName.class, collectionField.getName() )
									.bind( CodeTemplates.FieldValue.class, collectionField )
									.to( CodeTemplates.CollectionGetCollectionClearDirtyNames.class )
									.wrap( clearDirtyNames );
						}
					}
				}

				if ( enhancementContext.hasLazyLoadableAttributes( managedCtClass ) ) {
					clearDirtyNames = Advice.to( CodeTemplates.InitializeLazyAttributeLoadingInterceptor.class ).wrap( clearDirtyNames );
				}

				builder = builder.defineMethod( EnhancerConstants.TRACKER_COLLECTION_CHANGED_NAME, boolean.class, Visibility.PUBLIC )
						.intercept( isDirty )
						.defineMethod( EnhancerConstants.TRACKER_COLLECTION_CHANGED_FIELD_NAME, void.class, Visibility.PUBLIC )
						.withParameters( DirtyTracker.class )
						.intercept( getDirtyNames )
						.defineMethod( EnhancerConstants.TRACKER_COLLECTION_CLEAR_NAME, void.class, Visibility.PUBLIC )
						.intercept( Advice.withCustomMapping().to( CodeTemplates.ClearDirtyCollectionNames.class ).wrap( StubMethod.INSTANCE ) )
						.defineMethod( ExtendedSelfDirtinessTracker.REMOVE_DIRTY_FIELDS_NAME, void.class, Visibility.PUBLIC )
						.withParameters( LazyAttributeLoadingInterceptor.class )
						.intercept( clearDirtyNames );
			}

			return transformer.applyTo( builder, false );
		}
		else if ( enhancementContext.isCompositeClass( managedCtClass ) ) {
			log.infof( "Enhancing [%s] as Composite", managedCtClass.getName() );

			builder = builder.implement( ManagedComposite.class );
			builder = addInterceptorHandling( builder, managedCtClass );

			if ( enhancementContext.doDirtyCheckingInline( managedCtClass ) ) {
				builder = builder.implement( CompositeTracker.class )
						.defineField(
								EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME,
								CompositeOwnerTracker.class,
								FieldManifestation.TRANSIENT,
								Visibility.PRIVATE
						)
						.annotateField( AnnotationDescription.Builder.ofType( Transient.class ).build() )
						.defineMethod(
								EnhancerConstants.TRACKER_COMPOSITE_SET_OWNER,
								void.class,
								Visibility.PUBLIC
						)
						.withParameters( String.class, CompositeOwner.class )
						.intercept( Advice.to( CodeTemplates.SetOwner.class ).wrap( StubMethod.INSTANCE ) )
						.defineMethod(
								EnhancerConstants.TRACKER_COMPOSITE_CLEAR_OWNER,
								void.class,
								Visibility.PUBLIC
						)
						.withParameters( String.class )
						.intercept( Advice.to( CodeTemplates.ClearOwner.class ).wrap( StubMethod.INSTANCE ) );
			}

			return transformer.applyTo( builder, false );
		}
		else if ( enhancementContext.isMappedSuperclassClass( managedCtClass ) ) {
			log.infof( "Enhancing [%s] as MappedSuperclass", managedCtClass.getName() );

			builder = builder.implement( ManagedMappedSuperclass.class );
			return transformer.applyTo( builder, true );
		}
		else if ( enhancementContext.doExtendedEnhancement( managedCtClass ) ) {
			log.infof( "Extended enhancement of [%s]", managedCtClass.getName() );
			return transformer.applyExtended( builder );
		}
		else {
			log.debugf( "Skipping enhancement of [%s]: not entity or composite", managedCtClass.getName() );
			return null;
		}
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
		return builder.defineField( fieldName, type, Visibility.PRIVATE, FieldManifestation.TRANSIENT )
				.annotateField( AnnotationDescription.Builder.ofType( Transient.class ).build() )
				.defineMethod( getterName, type, Visibility.PUBLIC )
				.intercept( FieldAccessor.ofField( fieldName ) )
				.defineMethod( setterName, void.class, Visibility.PUBLIC )
				.withParameters( type )
				.intercept( FieldAccessor.ofField( fieldName ) );
	}

	private List<FieldDescription> collectCollectionFields(TypeDescription managedCtClass) {
		List<FieldDescription> collectionList = new ArrayList<>();

		for ( FieldDescription ctField : managedCtClass.getDeclaredFields() ) {
			// skip static fields and skip fields added by enhancement
			if ( Modifier.isStatic( ctField.getModifiers() ) || ctField.getName().startsWith( "$$_hibernate_" ) ) {
				continue;
			}
			if ( enhancementContext.isPersistentField( ctField ) ) {
				if ( ctField.getType().asErasure().isAssignableTo( Collection.class ) || ctField.getType().asErasure().isAssignableTo( Map.class ) ) {
					collectionList.add( ctField );
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

	private Collection<FieldDescription> collectInheritCollectionFields(TypeDefinition managedCtClass) {
		TypeDefinition managedCtSuperclass = managedCtClass.getSuperClass();
		if ( managedCtSuperclass == null || managedCtSuperclass.represents( Object.class ) ) {
			return Collections.emptyList();
		}

		if ( !enhancementContext.isMappedSuperclassClass( managedCtSuperclass.asErasure() ) ) {
			return collectInheritCollectionFields( managedCtSuperclass.asErasure() );
		}
		List<FieldDescription> collectionList = new ArrayList<FieldDescription>();

		for ( FieldDescription ctField : managedCtSuperclass.getDeclaredFields() ) {
			if ( !Modifier.isStatic( ctField.getModifiers() ) && enhancementContext.isPersistentField( ctField ) ) {
				if ( ctField.getType().asErasure().isAssignableTo( Collection.class ) || ctField.getType().asErasure().isAssignableTo( Map.class ) ) {
					collectionList.add( ctField );
				}
			}
		}
		collectionList.addAll( collectInheritCollectionFields( managedCtSuperclass ) );
		return collectionList;
	}

	static String capitalize(String value) {
		return Character.toUpperCase( value.charAt( 0 ) ) + value.substring( 1 );
	}

	static boolean isAnnotationPresent(FieldDescription fieldDescription, Class<? extends Annotation> type) {
		return getAnnotation( fieldDescription, type ) != null;
	}

	static <T extends Annotation> AnnotationDescription.Loadable<T> getAnnotation(FieldDescription fieldDescription, Class<T> type) {
		AnnotationDescription.Loadable<Access> access = fieldDescription.getDeclaringType().asErasure().getDeclaredAnnotations().ofType( Access.class );
		if ( access != null && access.loadSilent().value() == AccessType.PROPERTY ) {
			MethodDescription getter = getterOf( fieldDescription );
			if ( getter == null ) {
				return fieldDescription.getDeclaredAnnotations().ofType( type );
			}
			else {
				return getter.getDeclaredAnnotations().ofType( type );
			}
		}
		else if ( access != null && access.loadSilent().value() == AccessType.FIELD ) {
			return fieldDescription.getDeclaredAnnotations().ofType( type );
		}
		else {
			MethodDescription getter = getterOf( fieldDescription );
			if ( getter != null ) {
				AnnotationDescription.Loadable<T> annotationDescription = getter.getDeclaredAnnotations().ofType( type );
				if ( annotationDescription != null ) {
					return annotationDescription;
				}
			}
			return fieldDescription.getDeclaredAnnotations().ofType( type );
		}
	}

	static MethodDescription getterOf(FieldDescription persistentField) {
		MethodList<?> methodList = MethodGraph.Compiler.DEFAULT.compile( persistentField.getDeclaringType().asErasure() )
				.listNodes()
				.asMethodList()
				.filter( isGetter(persistentField.getName() ) );
		if ( methodList.size() == 1 ) {
			return methodList.getOnly();
		}
		else {
			return null;
		}
	}
}
