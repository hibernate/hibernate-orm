/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.boot.models.categorize.spi.ClassAttributeAccessType;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.models.categorize.spi.JpaEventListener;
import org.hibernate.boot.models.categorize.spi.JpaEventListenerStyle;
import org.hibernate.boot.models.categorize.spi.MappedSuperclassTypeMetadata;
import org.hibernate.boot.models.categorize.spi.ModelCategorizationContext;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;

import jakarta.persistence.AccessType;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.ExcludeDefaultListeners;
import jakarta.persistence.ExcludeSuperclassListeners;


/**
 * @author Steve Ebersole
 */
public abstract class AbstractIdentifiableTypeMetadata
		extends AbstractManagedTypeMetadata
		implements IdentifiableTypeMetadata {
	private final EntityHierarchy hierarchy;
	private final IdentifiableTypeMetadata superType;
	private final Set<IdentifiableTypeMetadata> subTypes = new HashSet<>();
	private final ClassAttributeAccessType classLevelAccessType;

	/**
	 * Used when creating the hierarchy root-root
	 *
	 * @param implicitAccessType This is the hierarchy default
	 */
	public AbstractIdentifiableTypeMetadata(
			ClassDetails classDetails,
			EntityHierarchy hierarchy,
			MappedSuperclassTypeMetadata superTypeMetadata,
			AccessType implicitAccessType,
			ModelCategorizationContext processingContext) {
		super( classDetails, processingContext );

		this.hierarchy = hierarchy;
		this.superType = superTypeMetadata;

		this.classLevelAccessType = CategorizationHelper.determineAccessType( classDetails, implicitAccessType );
	}


	public AbstractIdentifiableTypeMetadata(
			ClassDetails classDetails,
			EntityHierarchy hierarchy,
			IdentifiableTypeMetadata superType,
			ModelCategorizationContext processingContext) {
		super( classDetails, processingContext );

		assert superType != null;

		this.hierarchy = hierarchy;
		this.superType = superType;

		// this is arguably more logical, but the specification is very clear that this should come
		// from the hierarchy default not the super in section _2.3.2 Explicit Access Type_
		//this.accessType = CategorizationHelper.determineAccessType( classDetails, superType.getAccessType() );
		this.classLevelAccessType = CategorizationHelper.determineAccessType( classDetails, hierarchy.getDefaultAccessType() );
	}

	protected void postInstantiate(boolean rootEntityOrSubclass, HierarchyTypeConsumer typeConsumer) {
		typeConsumer.acceptType( this );

		// now we can effectively walk subs, although we skip that for the mapped-superclasses
		// "above" the root entity
		if ( rootEntityOrSubclass ) {
			walkSubclasses( typeConsumer );
		}

		// the idea here is to collect up class-level annotations and to apply
		// the maps from supers
		collectConversionInfo();
		collectAttributeOverrides();
		collectAssociationOverrides();
	}

	private void walkSubclasses(HierarchyTypeConsumer typeConsumer) {
		walkSubclasses( getClassDetails(), typeConsumer );
	}

	private void walkSubclasses(ClassDetails base, HierarchyTypeConsumer typeConsumer) {
		final ClassDetailsRegistry classDetailsRegistry = getModelContext().getClassDetailsRegistry();
		classDetailsRegistry.forEachDirectSubType( base.getName(), (subClassDetails) -> {
			final AbstractIdentifiableTypeMetadata subTypeMetadata;
			if ( CategorizationHelper.isEntity( subClassDetails ) ) {
				subTypeMetadata = new EntityTypeMetadataImpl(
						subClassDetails,
						getHierarchy(),
						this,
						typeConsumer,
						getModelContext()
				);
				addSubclass( subTypeMetadata );
			}
			else if ( CategorizationHelper.isMappedSuperclass( subClassDetails ) ) {
				subTypeMetadata = new MappedSuperclassTypeMetadataImpl(
						subClassDetails,
						getHierarchy(),
						this,
						typeConsumer,
						getModelContext()
				);
				addSubclass( subTypeMetadata );
			}
			else {
				// skip over "intermediate" sub-types
				walkSubclasses( subClassDetails, typeConsumer );
			}
		} );

	}

	protected void addSubclass(IdentifiableTypeMetadata subclass) {
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
	public ClassAttributeAccessType getClassLevelAccessType() {
		return classLevelAccessType;
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

		if ( classDetails.getAnnotationUsage( ExcludeSuperclassListeners.class ) == null ) {
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

		final AnnotationUsage<EntityListeners> entityListenersAnnotation = classDetails.getAnnotationUsage( EntityListeners.class );
		if ( entityListenersAnnotation == null ) {
			return;
		}

		final List<ClassDetails> entityListenerClasses = entityListenersAnnotation.getAttributeValue( "value" );
		if ( CollectionHelper.isEmpty( entityListenerClasses ) ) {
			return;
		}

		entityListenerClasses.forEach( (listenerClass) -> {
			consumer.accept( JpaEventListener.from( JpaEventListenerStyle.LISTENER, listenerClass ) );
		} );
	}

	protected List<JpaEventListener> collectCompleteEventListeners(ModelCategorizationContext modelContext) {
		final ClassDetails classDetails = getClassDetails();
		if ( classDetails.getAnnotationUsage( ExcludeDefaultListeners.class ) != null ) {
			return getHierarchyJpaEventListeners();
		}

		final List<JpaEventListener> combined = new ArrayList<>();
		combined.addAll( modelContext.getDefaultEventListeners() );
		combined.addAll( getHierarchyJpaEventListeners() );
		return combined;
	}
}
