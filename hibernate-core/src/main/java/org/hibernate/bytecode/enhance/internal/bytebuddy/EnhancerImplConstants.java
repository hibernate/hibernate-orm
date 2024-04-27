/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.StubMethod;

/**
 * Extracts constants used by EnhancerImpl.
 * This allows integrators to choose reusing this state for multiple enhancement processes,
 * as these are fairly expensive to initialize, or rather choose to free memory by
 * not retaining this.
 */
public final class EnhancerImplConstants {

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
	}

}
