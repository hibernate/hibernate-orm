/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import java.util.List;

import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.JpaEventListener;
import org.hibernate.boot.models.categorize.spi.MappedSuperclassTypeMetadata;
import org.hibernate.boot.models.categorize.spi.ModelCategorizationContext;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.AccessType;

/**
 * @author Steve Ebersole
 */
public class MappedSuperclassTypeMetadataImpl
		extends AbstractIdentifiableTypeMetadata
		implements MappedSuperclassTypeMetadata {

	private final List<AttributeMetadata> attributeList;
	private final List<JpaEventListener> hierarchyEventListeners;
	private final List<JpaEventListener> completeEventListeners;

	/**
	 * Form used when the mapped-superclass is the absolute-root of the hierarchy ("above" the root entity)
	 */
	public MappedSuperclassTypeMetadataImpl(
			ClassDetails classDetails,
			EntityHierarchy hierarchy,
			MappedSuperclassTypeMetadataImpl superTypeMetadata,
			AccessType defaultAccessType,
			HierarchyTypeConsumer typeConsumer,
			ModelCategorizationContext modelContext) {
		super( classDetails, hierarchy, superTypeMetadata, defaultAccessType, modelContext );

		final LifecycleCallbackCollector lifecycleCallbackCollector = new LifecycleCallbackCollector( classDetails, modelContext );
		this.attributeList = resolveAttributes( lifecycleCallbackCollector );
		this.hierarchyEventListeners = collectHierarchyEventListeners( lifecycleCallbackCollector.resolve() );
		this.completeEventListeners = collectCompleteEventListeners( modelContext );

		if ( superTypeMetadata != null ) {
			superTypeMetadata.addSubclass( this );
		}

		postInstantiate( false, typeConsumer );
	}



	/**
	 * Form used when the mapped-superclass is NOT the absolute-root of the hierarchy
	 */
	public MappedSuperclassTypeMetadataImpl(
			ClassDetails classDetails,
			EntityHierarchy hierarchy,
			AbstractIdentifiableTypeMetadata superType,
			HierarchyTypeConsumer typeConsumer,
			ModelCategorizationContext modelContext) {
		super( classDetails, hierarchy, superType, modelContext );

		final LifecycleCallbackCollector lifecycleCallbackCollector = new LifecycleCallbackCollector( classDetails, modelContext );
		this.attributeList = resolveAttributes( lifecycleCallbackCollector );
		this.hierarchyEventListeners = collectHierarchyEventListeners( lifecycleCallbackCollector.resolve() );
		this.completeEventListeners = collectCompleteEventListeners( modelContext );

		postInstantiate( true, typeConsumer );
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
