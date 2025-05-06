/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import jakarta.persistence.Transient;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.modifier.FieldPersistence;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.StubMethod;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import org.hibernate.Version;
import org.hibernate.bytecode.enhance.spi.CollectionTracker;
import org.hibernate.bytecode.enhance.spi.EnhancementInfo;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;

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
	final int methodModifierPUBLIC = ModifierContributor.Resolver.of( List.of( Visibility.PUBLIC ) ).resolve();
	final int fieldModifierPRIVATE_TRANSIENT = ModifierContributor.Resolver.of( List.of( FieldPersistence.TRANSIENT, Visibility.PRIVATE ) ).resolve();

	//Frequently used annotations, declared as collections as otherwise they get wrapped into them over and over again:
	final Collection<? extends AnnotationDescription> TRANSIENT_ANNOTATION = List.of(
			AnnotationDescription.Builder.ofType( Transient.class ).build() );
	final List<Annotation> HIBERNATE_VERSION_ANNOTATION = List.of( new EnhancementInfo() {
		@Override
		public String version() {
			return Version.getVersionString();
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return EnhancementInfo.class;
		}
	} );

	//Frequently used Types for method signatures:
	final TypeDefinition TypeVoid = TypeDescription.ForLoadedType.of( void.class );
	final TypeDefinition TypeBooleanPrimitive = TypeDescription.ForLoadedType.of( boolean.class );
	final TypeDefinition TypeIntegerPrimitive = TypeDescription.ForLoadedType.of( int.class );
	final TypeDefinition TypeManagedEntity = TypeDescription.ForLoadedType.of( ManagedEntity.class );
	final TypeDefinition TypeEntityEntry = TypeDescription.ForLoadedType.of( EntityEntry.class );
	final TypeDefinition TypePersistentAttributeInterceptor = TypeDescription.ForLoadedType.of( PersistentAttributeInterceptor.class );
	final TypeDefinition TypeObject = TypeDescription.ForLoadedType.of( Object.class );
	final TypeDefinition Type_Array_String = TypeDescription.ForLoadedType.of( String[].class );
	final TypeDefinition TypeCollectionTracker = TypeDescription.ForLoadedType.of( CollectionTracker.class );

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

}
