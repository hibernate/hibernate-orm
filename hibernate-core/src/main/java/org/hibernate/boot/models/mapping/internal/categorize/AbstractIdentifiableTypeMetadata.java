/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.categorize;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.ExcludeDefaultListeners;
import jakarta.persistence.ExcludeSuperclassListeners;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/// Abstract IdentifiableTypeMetadata impl
///
/// @since 9.0
/// @author Steve Ebersole
public abstract class AbstractIdentifiableTypeMetadata
		extends AbstractManagedTypeMetadata
		implements IdentifiableTypeMetadata {
	private final EntityHierarchy hierarchy;
	private final ManagedTypeInheritanceState inheritanceState;
	private final AbstractIdentifiableTypeMetadata superType;
	private final Set<IdentifiableTypeMetadata> subTypes = new HashSet<>();
	private final AccessType accessType;

	/**
	 * Used when creating the hierarchy root-root
	 */
	public AbstractIdentifiableTypeMetadata(
			ClassDetails classDetails,
			EntityHierarchy hierarchy,
			ManagedTypeInheritanceState inheritanceState,
			CategorizationContext processingContext) {
		super( classDetails, processingContext );

		this.hierarchy = hierarchy;
		this.inheritanceState = inheritanceState;
		this.superType = null;

		this.accessType = determineAccessType();
	}


	public AbstractIdentifiableTypeMetadata(
			ClassDetails classDetails,
			EntityHierarchy hierarchy,
			AbstractIdentifiableTypeMetadata superType,
			ManagedTypeInheritanceState inheritanceState,
			CategorizationContext processingContext) {
		super( classDetails, processingContext );

		assert superType != null;

		this.hierarchy = hierarchy;
		this.inheritanceState = inheritanceState;
		this.superType = superType;

		this.accessType = determineAccessType();
	}

	protected void postInstantiate(HierarchyMetadataCollector metadataCollector) {
		metadataCollector.collectType( this );

		// now we can effectively walk subs
		walkSubclasses( metadataCollector );

		// the idea here is to collect up class-level annotations and to apply
		// the maps from supers
		collectConversionInfo();
		collectAttributeOverrides();
		collectAssociationOverrides();
	}

	private void walkSubclasses(HierarchyMetadataCollector metadataCollector) {
		walkSubclasses( getClassDetails(), metadataCollector );
	}

	private void walkSubclasses(ClassDetails base, HierarchyMetadataCollector metadataCollector) {
		if ( inheritanceState != null ) {
			inheritanceState.forEachSubType( base, (subClassDetails) -> processSubclass( subClassDetails, metadataCollector ) );
			return;
		}

		final ClassDetailsRegistry classDetailsRegistry = getModelContext().getClassDetailsRegistry();
		classDetailsRegistry.forEachDirectSubType( base.getName(), (subClassDetails) -> {
			if ( !processSubclass( subClassDetails, metadataCollector ) ) {
				// skip over "intermediate" sub-types
				walkSubclasses( subClassDetails, metadataCollector );
			}
		} );

	}

	private boolean processSubclass(ClassDetails subClassDetails, HierarchyMetadataCollector metadataCollector) {
		if ( !metadataCollector.shouldProcessSubType( getClassDetails(), subClassDetails ) ) {
			return true;
		}

		final AbstractIdentifiableTypeMetadata subTypeMetadata;
		if ( CategorizationHelper.isEntity( subClassDetails ) ) {
			subTypeMetadata = new EntityTypeMetadataImpl(
					subClassDetails,
					getHierarchy(),
					this,
					inheritanceState,
					metadataCollector,
					getModelContext()
			);
			addSubclass( subTypeMetadata );
			return true;
		}
		else if ( CategorizationHelper.isMappedSuperclass( subClassDetails ) ) {
			subTypeMetadata = new MappedSuperclassTypeMetadataImpl(
					subClassDetails,
					getHierarchy(),
					this,
					inheritanceState,
					metadataCollector,
					getModelContext()
			);
			addSubclass( subTypeMetadata );
			return true;
		}

		return false;
	}

	private AccessType determineAccessType() {
		final Access annotation = getClassDetails().getDirectAnnotationUsage( Access.class );
		if ( annotation != null ) {
			return annotation.value();
		}

		return hierarchy.getDefaultAccessType();
	}

	private void addSubclass(IdentifiableTypeMetadata subclass) {
		subTypes.add( subclass );
	}

	@Override
	public EntityHierarchy getHierarchy() {
		return hierarchy;
	}

	@Override
	public IdentifiableTypeMetadata getSuperType() {
		return superType;
	}

	@Override
	public boolean isAbstract() {
		return getClassDetails().isAbstract();
	}

	@Override
	public boolean hasSubTypes() {
		// assume this is called only after its constructor is complete
		return !subTypes.isEmpty();
	}

	@Override
	public int getNumberOfSubTypes() {
		return subTypes.size();
	}

	@Override
	public void forEachSubType(Consumer<IdentifiableTypeMetadata> consumer) {
		// assume this is called only after its constructor is complete
		subTypes.forEach( consumer );
	}

	@Override
	public Iterable<IdentifiableTypeMetadata> getSubTypes() {
		// assume this is called only after its constructor is complete
		return subTypes;
	}

	@Override
	public AccessType getAccessType() {
		return accessType;
	}

	protected void collectConversionInfo() {
		// we only need to do this on root
	}

	protected void collectAttributeOverrides() {
		// we only need to do this on root
	}

	protected void collectAssociationOverrides() {
		// we only need to do this on root
	}

	protected List<JpaEventListener> collectHierarchyEventListeners(JpaEventListener localCallback) {
		final ClassDetails classDetails = getClassDetails();

		final List<JpaEventListener> combined = new ArrayList<>();

		if ( classDetails.getDirectAnnotationUsage( ExcludeSuperclassListeners.class ) == null ) {
			final IdentifiableTypeMetadata superType = getSuperType();
			if ( superType != null ) {
				combined.addAll( superType.getHierarchyJpaEventListeners() );
			}
		}

		applyLocalEventListeners( combined::add );

		if ( localCallback != null ) {
			combined.add( localCallback );
		}

		return combined;
	}

	private void applyLocalEventListeners(Consumer<JpaEventListener> consumer) {
		final ClassDetails classDetails = getClassDetails();

		final EntityListeners entityListenersAnnotation = classDetails.getDirectAnnotationUsage( EntityListeners.class );
		if ( entityListenersAnnotation == null ) {
			return;
		}

		final Class<?>[] entityListenerClasses = entityListenersAnnotation.value();
		if ( entityListenerClasses == null || entityListenerClasses.length == 0 ) {
			return;
		}

		for ( Class<?> listenerClass : entityListenerClasses ) {
			consumer.accept( JpaEventListener.from(
					JpaEventListenerStyle.LISTENER,
					getModelContext().getClassDetailsRegistry().resolveClassDetails( listenerClass.getName() )
			) );
		}
	}

	protected List<JpaEventListener> collectCompleteEventListeners(CategorizationContext modelContext) {
		final ClassDetails classDetails = getClassDetails();
		if ( classDetails.getDirectAnnotationUsage( ExcludeDefaultListeners.class ) != null ) {
			return getHierarchyJpaEventListeners();
		}

		final List<JpaEventListener> combined = new ArrayList<>();
		combined.addAll( modelContext.getDefaultEventListeners() );
		combined.addAll( getHierarchyJpaEventListeners() );
		return combined;
	}
}
