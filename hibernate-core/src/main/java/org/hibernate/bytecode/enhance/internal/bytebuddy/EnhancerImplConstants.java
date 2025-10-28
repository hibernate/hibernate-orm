/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import jakarta.persistence.Transient;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.FieldPersistence;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.StubMethod;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.hibernate.bytecode.enhance.internal.tracker.CompositeOwnerTracker;
import org.hibernate.bytecode.enhance.internal.tracker.DirtyTracker;
import org.hibernate.bytecode.enhance.spi.CollectionTracker;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.engine.spi.CompositeOwner;
import org.hibernate.engine.spi.CompositeTracker;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ExtendedSelfDirtinessTracker;
import org.hibernate.engine.spi.ManagedComposite;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.ManagedMappedSuperclass;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.proxy.ProxyConfiguration;

import static net.bytebuddy.matcher.ElementMatchers.isDefaultFinalizer;

/**
 * Extracts constants used by EnhancerImpl.
 * This allows integrators to choose reusing this state for multiple enhancement processes,
 * as these are fairly expensive to initialize, or rather choose to free memory by
 * not retaining this.
 */
public final class EnhancerImplConstants {

	//This locator is used to load all "advice", and apparently won't benefit from any caches:
	final ClassFileLocator adviceLocator;

	final Implementation implementationTrackChange;
	final Implementation implementationGetDirtyAttributesWithoutCollections;
	final Implementation implementationAreFieldsDirtyWithoutCollections;
	final Implementation implementationClearDirtyAttributesWithoutCollections;
	final Implementation implementationSuspendDirtyTracking;
	final Implementation implementationGetDirtyAttributes;
	final Implementation implementationAreFieldsDirty;
	final Implementation implementationGetCollectionTrackerWithoutCollections;
	final Implementation implementationClearDirtyAttributes;
	//In this case we just extract the Advice:
	final Advice adviceInitializeLazyAttributeLoadingInterceptor;
	final Implementation implementationSetOwner;
	final Implementation implementationClearOwner;
	final Implementation implementationSetPersistenceInfo;

	//Frequently used Modifiers:
	final int modifierPUBLIC = ModifierContributor.Resolver.of( List.of( Visibility.PUBLIC ) ).resolve();
	public final int modifierPRIVATE = ModifierContributor.Resolver.of( List.of( Visibility.PRIVATE ) ).resolve();
	final int modifierPRIVATE_TRANSIENT = ModifierContributor.Resolver.of( List.of( FieldPersistence.TRANSIENT, Visibility.PRIVATE ) ).resolve();

	//Frequently used annotations, declared as collections as otherwise they get wrapped into them over and over again:
	final Collection<? extends AnnotationDescription> TRANSIENT_ANNOTATION = List.of(
			AnnotationDescription.Builder.ofType( Transient.class ).build() );

	//Frequently used Types for method signatures:
	final TypeDefinition TypeVoid = TypeDescription.ForLoadedType.of( void.class );
	final TypeDefinition TypeBooleanPrimitive = TypeDescription.ForLoadedType.of( boolean.class );
	final TypeDefinition TypeIntegerPrimitive = TypeDescription.ForLoadedType.of( int.class );
	public final TypeDefinition TypeProxyConfiguration_Interceptor = TypeDescription.ForLoadedType.of( ProxyConfiguration.Interceptor.class );
	final TypeDefinition TypeManagedEntity = TypeDescription.ForLoadedType.of( ManagedEntity.class );
	final TypeDefinition TypeEntityEntry = TypeDescription.ForLoadedType.of( EntityEntry.class );
	final TypeDefinition TypePersistentAttributeInterceptor = TypeDescription.ForLoadedType.of( PersistentAttributeInterceptor.class );
	public final TypeDefinition TypeObject = TypeDescription.ForLoadedType.of( Object.class );
	final TypeDefinition TypeString = TypeDescription.ForLoadedType.of( String.class );
	final TypeDefinition Type_Array_String = TypeDescription.ForLoadedType.of( String[].class );
	final TypeDefinition TypeCollectionTracker = TypeDescription.ForLoadedType.of( CollectionTracker.class );
	final TypeDefinition Type_Array_Object = TypeDescription.ForLoadedType.of( Object[].class );
	final TypeDefinition TypeLazyAttributeLoadingInterceptor = TypeDescription.ForLoadedType.of(
			LazyAttributeLoadingInterceptor.class );
	final TypeDefinition TypeCompositeOwnerTracker = TypeDescription.ForLoadedType.of( CompositeOwnerTracker.class );
	public final TypeDefinition TypeInstantiationOptimizer = TypeDescription.ForLoadedType.of(
			ReflectionOptimizer.InstantiationOptimizer.class );

	//Careful with the following types being used: the ByteBuddy receiver often supports overloading, and using
	//one of the other specific types in the declaration might lead to unexpectedly invoking a different method.
	final List<TypeDefinition> INTERFACES_for_PersistentAttributeInterceptor = List.of( TypePersistentAttributeInterceptor );
	public final Collection<? extends TypeDefinition> INTERFACES_for_AccessOptimizer = List.of(
			TypeDescription.ForLoadedType.of( ReflectionOptimizer.AccessOptimizer.class )
	);
	public final Collection<? extends TypeDefinition> INTERFACES_for_SelfDirtinessTracker = List.of(
			TypeDescription.ForLoadedType.of( SelfDirtinessTracker.class )
	);
	public final Collection<? extends TypeDefinition> INTERFACES_for_ExtendedSelfDirtinessTracker = List.of(
			TypeDescription.ForLoadedType.of( ExtendedSelfDirtinessTracker.class )
	);
	public final Collection<? extends TypeDefinition> INTERFACES_for_ManagedComposite = List.of(
			TypeDescription.ForLoadedType.of( ManagedComposite.class )
	);
	public final Collection<? extends TypeDefinition> INTERFACES_for_ManagedMappedSuperclass = List.of(
			TypeDescription.ForLoadedType.of( ManagedMappedSuperclass.class )
	);
	public final Collection<? extends TypeDefinition> INTERFACES_for_CompositeOwner = List.of(
			TypeDescription.ForLoadedType.of( CompositeOwner.class )
	);
	public final Collection<? extends TypeDefinition> INTERFACES_for_CompositeTracker = List.of(
			TypeDescription.ForLoadedType.of( CompositeTracker.class )
	);
	public final Collection<? extends TypeDefinition> INTERFACES_for_PersistentAttributeInterceptable = List.of(
			TypeDescription.ForLoadedType.of( PersistentAttributeInterceptable.class )
	);
	public final Collection<? extends TypeDefinition> INTERFACES_for_ProxyConfiguration = List.of(
			TypeDescription.ForLoadedType.of( ProxyConfiguration.class )
	);
	public final Collection<? extends TypeDefinition> INTERFACES_for_ManagedEntity = List.of( TypeManagedEntity	);

	//Frequently used ElementMatchers:
	final ElementMatcher.Junction<MethodDescription> DEFAULT_FINALIZER = isDefaultFinalizer();
	public final ElementMatcher.Junction<NamedElement> newInstanceMethodName = ElementMatchers.named( "newInstance" );
	public final ElementMatcher.Junction<NamedElement> getPropertyValuesMethodName = ElementMatchers.named(	"getPropertyValues" );
	public final ElementMatcher.Junction<NamedElement> setPropertyValuesMethodName = ElementMatchers.named(	"setPropertyValues" );
	public final ElementMatcher.Junction<NamedElement> getPropertyNamesMethodName = ElementMatchers.named( "getPropertyNames" );

	//Frequently used types for field definitions:
	final TypeDescription.Generic DirtyTrackerTypeDescription = TypeDefinition.Sort.describe( DirtyTracker.class );

	//Internal names:
	final String internalName_CompositeTracker = Type.getInternalName( CompositeTracker.class );
	final String internalName_LazyPropertyInitializer = Type.getInternalName( LazyPropertyInitializer.class );
	final String internalName_Object = Type.getInternalName( Object.class );
	final String internalName_String = Type.getInternalName( String.class );
	final String internalName_BytecodeLazyAttributeInterceptor = Type.getInternalName( BytecodeLazyAttributeInterceptor.class );
	final String internalName_LazyAttributeLoadingInterceptor = Type.getInternalName(
			LazyAttributeLoadingInterceptor.class );
	final String internalName_PersistentAttributeInterceptor = Type.getInternalName(
			PersistentAttributeInterceptor.class );

	//Method Descriptors:
	final String methodDescriptor_SetOwner = Type.getMethodDescriptor(
			Type.getType( void.class ),
			Type.getType( String.class ),
			Type.getType( CompositeOwner.class )
	);
	final String methodDescriptor_getInterceptor = Type.getMethodDescriptor(
			Type.getType( PersistentAttributeInterceptor.class )
	);
	final String methodDescriptor_attributeInitialized = Type.getMethodDescriptor(
			Type.getType( void.class ),
			Type.getType( String.class )
	);
	final String methodDescriptor_isAttributeLoaded = Type.getMethodDescriptor(
			Type.getType( boolean.class ),
			Type.getType( String.class )
	);

	//Others :
	final String Serializable_TYPE_DESCRIPTOR = Type.getDescriptor( Serializable.class );

	public EnhancerImplConstants() {
		this.adviceLocator = ClassFileLocator.ForClassLoader.of( CodeTemplates.class.getClassLoader() );
		this.implementationTrackChange = Advice.to( CodeTemplates.TrackChange.class, adviceLocator )
				.wrap( StubMethod.INSTANCE );
		this.implementationGetDirtyAttributesWithoutCollections = Advice.to(
				CodeTemplates.GetDirtyAttributesWithoutCollections.class,
				adviceLocator
		).wrap( StubMethod.INSTANCE );
		this.implementationAreFieldsDirtyWithoutCollections = Advice.to(
				CodeTemplates.AreFieldsDirtyWithoutCollections.class,
				adviceLocator
		).wrap( StubMethod.INSTANCE );
		this.implementationClearDirtyAttributesWithoutCollections = Advice.to(
				CodeTemplates.ClearDirtyAttributesWithoutCollections.class,
				adviceLocator
		).wrap( StubMethod.INSTANCE );
		this.implementationSuspendDirtyTracking = Advice.to( CodeTemplates.SuspendDirtyTracking.class, adviceLocator )
				.wrap( StubMethod.INSTANCE );
		this.implementationGetDirtyAttributes = Advice.to( CodeTemplates.GetDirtyAttributes.class, adviceLocator ).wrap(
				StubMethod.INSTANCE );
		this.implementationAreFieldsDirty = Advice.to( CodeTemplates.AreFieldsDirty.class, adviceLocator ).wrap(
				StubMethod.INSTANCE );
		this.implementationGetCollectionTrackerWithoutCollections = Advice.to(
				CodeTemplates.GetCollectionTrackerWithoutCollections.class,
				adviceLocator
		).wrap( StubMethod.INSTANCE );
		this.implementationClearDirtyAttributes = Advice.to( CodeTemplates.ClearDirtyAttributes.class, adviceLocator )
				.wrap( StubMethod.INSTANCE );
		//In this case we just extract the Advice:
		this.adviceInitializeLazyAttributeLoadingInterceptor = Advice.to(
				CodeTemplates.InitializeLazyAttributeLoadingInterceptor.class,
				adviceLocator
		);
		this.implementationSetOwner = Advice.to( CodeTemplates.SetOwner.class, adviceLocator )
				.wrap( StubMethod.INSTANCE );
		this.implementationClearOwner = Advice.to( CodeTemplates.ClearOwner.class, adviceLocator )
				.wrap( StubMethod.INSTANCE );
		this.implementationSetPersistenceInfo = Advice.to( CodeTemplates.SetPersistenceInfo.class, adviceLocator )
				.wrap( StubMethod.INSTANCE );
	}

	public ElementMatcher<? super MethodDescription> defaultFinalizer() {
		return DEFAULT_FINALIZER;
	}

}
