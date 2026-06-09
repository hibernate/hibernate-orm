/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.util.List;

import org.hibernate.models.spi.ClassDetails;

/// Standard MappedSuperclassTypeMetadata impl
///
/// @since 9.0
/// @author Steve Ebersole
public class MappedSuperclassTypeMetadataImpl
		extends AbstractIdentifiableTypeMetadata
		implements MappedSuperclassTypeMetadata {

	private final List<AttributeMetadata> attributeList;
	private final List<JpaEventListener> hierarchyEventListeners;
	private final List<JpaEventListener> completeEventListeners;

	public MappedSuperclassTypeMetadataImpl(
			ClassDetails classDetails,
			EntityHierarchy hierarchy,
			ManagedTypeInheritanceState inheritanceState,
			HierarchyMetadataCollector metadataCollector,
			CategorizationContext modelContext) {
		super( classDetails, hierarchy, inheritanceState, modelContext );

		final LifecycleCallbackCollector lifecycleCallbackCollector = new LifecycleCallbackCollector( classDetails );
		this.attributeList = resolveAttributes( lifecycleCallbackCollector );
		this.hierarchyEventListeners = collectHierarchyEventListeners( lifecycleCallbackCollector.resolve() );
		this.completeEventListeners = collectCompleteEventListeners( modelContext );

		postInstantiate( metadataCollector );
	}

	public MappedSuperclassTypeMetadataImpl(
			ClassDetails classDetails,
			EntityHierarchy hierarchy,
			AbstractIdentifiableTypeMetadata superType,
			ManagedTypeInheritanceState inheritanceState,
			HierarchyMetadataCollector metadataCollector,
			CategorizationContext modelContext) {
		super( classDetails, hierarchy, superType, inheritanceState, modelContext );

		final LifecycleCallbackCollector lifecycleCallbackCollector = new LifecycleCallbackCollector( classDetails );
		this.attributeList = resolveAttributes( lifecycleCallbackCollector );
		this.hierarchyEventListeners = collectHierarchyEventListeners( lifecycleCallbackCollector.resolve() );
		this.completeEventListeners = collectCompleteEventListeners( modelContext );

		postInstantiate( metadataCollector );
	}

	@Override
	protected List<AttributeMetadata> attributeList() {
		return attributeList;
	}

	@Override
	public List<JpaEventListener> getHierarchyJpaEventListeners() {
		return hierarchyEventListeners;
	}

	@Override
	public List<JpaEventListener> getCompleteJpaEventListeners() {
		return completeEventListeners;
	}
}
