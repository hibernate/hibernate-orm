/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.internal.AbstractInitializer;
import org.hibernate.sql.results.internal.NullValueAssembler;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.sql.results.graph.entity.internal.BatchEntityInsideEmbeddableSelectFetchInitializer.BATCH_PROPERTY;

/**
 * @author Steve Ebersole
 */
public class NonAggregatedIdentifierMappingInitializer
		extends AbstractInitializer<NonAggregatedIdentifierMappingInitializer.NonAggregatedIdentifierMappingInitializerData>
		implements EmbeddableInitializer<NonAggregatedIdentifierMappingInitializer.NonAggregatedIdentifierMappingInitializerData> {

	private final NavigablePath navigablePath;
	private final NonAggregatedIdentifierMapping embedded;
	private final EmbeddableMappingType virtualIdEmbeddable;
	private final EmbeddableMappingType representationEmbeddable;
	private final EmbeddableInstantiator embeddableInstantiator;
	private final @Nullable InitializerParent<?> parent;
	private final boolean isResultInitializer;

	private final DomainResultAssembler<?>[] assemblers;
	private final @Nullable Initializer<InitializerData>[] initializers;
	private final @Nullable Initializer<InitializerData>[] subInitializersForResolveFromInitialized;
	private final @Nullable Initializer<InitializerData>[] collectionContainingSubInitializers;
	private final boolean lazyCapable;
	private final boolean hasLazySubInitializer;
	private final boolean hasIdClass;

	public static class NonAggregatedIdentifierMappingInitializerData extends InitializerData implements ValueAccess {
		protected final boolean isFindByIdLookup;
		protected final InitializerData parentData;
		protected final Object[] virtualIdState;
		protected final Object[] idClassState;

		public NonAggregatedIdentifierMappingInitializerData(
				NonAggregatedIdentifierMappingInitializer initializer,
				RowProcessingState rowProcessingState) {
			super( rowProcessingState );
			isFindByIdLookup = isIsFindByIdLookup( initializer, rowProcessingState );
			parentData = initializer.parent == null ? null : initializer.parent.getData( rowProcessingState );
			final var virtualIdEmbeddable = initializer.embedded.getEmbeddableTypeDescriptor();
			final int size = virtualIdEmbeddable.getNumberOfFetchables();
			virtualIdState = new Object[size];
			idClassState = new Object[size];
		}

		private boolean isIsFindByIdLookup(
				NonAggregatedIdentifierMappingInitializer initializer,
				RowProcessingState rowProcessingState) {
			return !initializer.hasIdClass
				&& rowProcessingState.getEntityId() != null
				&& initializer.navigablePath.getParent().getParent() == null
				&& initializer.navigablePath instanceof EntityIdentifierNavigablePath;
		}

		@Override
		public Object[] getValues() {
			assert getState() == State.RESOLVED;
			return idClassState;
		}

		@Override
		public <T> T getValue(int i, Class<T> clazz) {
			assert getState() == State.RESOLVED;
			return clazz.cast( idClassState[i] );
		}

		@Override
		public Object getOwner() {
			return parentData == null ? null : parentData.getInstance();
		}
	}

	public NonAggregatedIdentifierMappingInitializer(
			EmbeddableResultGraphNode resultDescriptor,
			InitializerParent<?> parent,
			AssemblerCreationState creationState,
			boolean isResultInitializer) {
		this( resultDescriptor, parent, creationState, isResultInitializer, Function.identity() );
	}

	protected NonAggregatedIdentifierMappingInitializer(
			EmbeddableResultGraphNode resultDescriptor,
			InitializerParent<?> parent,
			AssemblerCreationState creationState,
			boolean isResultInitializer,
			Function<Fetch, Fetch> fetchConverter) {
		super( creationState );
		this.parent = parent;
		this.isResultInitializer = isResultInitializer;

		navigablePath = resultDescriptor.getNavigablePath();
		embedded =
				(NonAggregatedIdentifierMapping)
						resultDescriptor.getReferencedMappingContainer();

		virtualIdEmbeddable = embedded.getEmbeddableTypeDescriptor();
		representationEmbeddable = embedded.getMappedIdEmbeddableTypeDescriptor();
		embeddableInstantiator = representationEmbeddable.getRepresentationStrategy().getInstantiator();
		hasIdClass = embedded.hasContainingClass() && virtualIdEmbeddable != representationEmbeddable;

		final int size = virtualIdEmbeddable.getNumberOfFetchables();
		final var assemblers = new DomainResultAssembler[size];
		this.assemblers = assemblers;
		final Initializer<?>[] initializers = new Initializer[assemblers.length];
//		final Initializer<?>[] eagerSubInitializers = new Initializer[assemblers.length];
		final Initializer<?>[] collectionContainingSubInitializers = new Initializer[assemblers.length];
		boolean empty = true;
		boolean emptyEager = true;
		boolean emptyCollectionInitializers = true;
		boolean lazyCapable = false;
		boolean hasLazySubInitializers = false;
		for ( int i = 0; i < size; i++ ) {
			final var stateArrayContributor = virtualIdEmbeddable.getFetchable( i );
			final var fetch = fetchConverter.apply( resultDescriptor.findFetch( stateArrayContributor ) );

			final var stateAssembler =
					fetch == null
							? new NullValueAssembler<>( stateArrayContributor.getJavaType() )
							: fetch.createAssembler( this, creationState );

			assemblers[i] = stateAssembler;

			final var initializer = stateAssembler.getInitializer();
			if ( initializer != null ) {
				if ( initializer.isEager() ) {
//					eagerSubInitializers[i] = initializer;
					hasLazySubInitializers = hasLazySubInitializers || initializer.hasLazySubInitializers();
					emptyEager = false;
					assert fetch != null;
					final var fetchParent = fetch.asFetchParent();
					if ( fetchParent != null && fetchParent.containsCollectionFetches()
							|| initializer.isCollectionInitializer() ) {
						collectionContainingSubInitializers[i] = initializer;
						emptyCollectionInitializers = false;
					}
				}
				else {
					hasLazySubInitializers = true;
				}
				lazyCapable = lazyCapable || initializer.isLazyCapable();
				initializers[i] = initializer;
				empty = false;
			}
		}
		//noinspection unchecked
		this.initializers = (Initializer<InitializerData>[]) (
				empty ? Initializer.EMPTY_ARRAY : initializers
		);
		// No need to think about bytecode enhancement here, since ids can't contain lazy basic attributes
		//noinspection unchecked
		this.subInitializersForResolveFromInitialized = (Initializer<InitializerData>[]) (
				emptyEager ? Initializer.EMPTY_ARRAY : initializers
		);
		//noinspection unchecked
		this.collectionContainingSubInitializers = (Initializer<InitializerData>[]) (
				emptyCollectionInitializers ? Initializer.EMPTY_ARRAY : collectionContainingSubInitializers
		);
		this.lazyCapable = lazyCapable;
		this.hasLazySubInitializer = hasLazySubInitializers;
	}

	@Override
	public EmbeddableValuedModelPart getInitializedPart() {
		return embedded;
	}

	@Override
	public @Nullable InitializerParent<?> getParent() {
		return parent;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public boolean isResultInitializer() {
		return isResultInitializer;
	}

	@Override
	protected InitializerData createInitializerData(RowProcessingState rowProcessingState) {
		return new NonAggregatedIdentifierMappingInitializerData( this, rowProcessingState );
	}

	@Override
	public void resolveKey(NonAggregatedIdentifierMappingInitializerData data) {
		if ( data.getState() == State.UNINITIALIZED ) {
			data.setInstance( null );
			data.setState( State.KEY_RESOLVED );
			if ( initializers.length == 0 ) {
				// Resolve the component early to know if the key is missing or not
				resolveInstance( data );
			}
			else {
				final var rowProcessingState = data.getRowProcessingState();
				for ( var initializer : initializers ) {
					if ( initializer != null ) {
						final var subData = initializer.getData( rowProcessingState );
						initializer.resolveKey( subData );
						if ( subData.getState() == State.MISSING ) {
							data.setState( State.MISSING );
							return;
						}
					}
				}
			}
		}
	}

	@Override
	public void resetResolvedEntityRegistrations(RowProcessingState rowProcessingState) {
		final var data = getData( rowProcessingState );
		for ( var initializer : initializers ) {
			if ( initializer != null ) {
				final var entityInitializer = initializer.asEntityInitializer();
				if ( entityInitializer != null ) {
					entityInitializer.resetResolvedEntityRegistrations( rowProcessingState );
				}
				else {
					final var embeddableInitializer = initializer.asEmbeddableInitializer();
					if ( embeddableInitializer != null ) {
						embeddableInitializer.resetResolvedEntityRegistrations( rowProcessingState );
					}
				}
			}
		}
	}

	@Override
	public void resolveFromPreviousRow(NonAggregatedIdentifierMappingInitializerData data) {
		if ( data.getState() == State.UNINITIALIZED ) {
			if ( data.getInstance() == null ) {
				data.setState( State.MISSING );
			}
			else {
				final var rowProcessingState = data.getRowProcessingState();
				// When a previous row initialized this entity already, we only need to process collections
				for ( var initializer : collectionContainingSubInitializers ) {
					if ( initializer != null ) {
						initializer.resolveFromPreviousRow( rowProcessingState );
					}
				}
				data.setState( State.INITIALIZED );
			}
		}
	}

	@Override
	public void resolveInstance(NonAggregatedIdentifierMappingInitializerData data) {
		if ( data.getState() == State.KEY_RESOLVED ) {// If we don't have an id class and this is a find by id lookup, we just use that instance
			if ( data.isFindByIdLookup ) {
				data.setInstance( data.getRowProcessingState().getEntityId() );
				data.setState( State.INITIALIZED );
			}
			else {
				data.setState( State.RESOLVED );
				// We need to possibly wrap the processing state if the embeddable is within an aggregate
				extractRowState( data );
				data.setInstance( data.getState() == State.MISSING ? null : embeddableInstantiator.instantiate( data ) );
				if ( parent == null ) {
					data.setState( State.INITIALIZED );
				}
			}
		}
	}

	@Override
	public void resolveInstance(@Nullable Object instance, NonAggregatedIdentifierMappingInitializerData data) {
		if ( instance == null ) {
			data.setState( State.MISSING );
			data.setInstance( null );
		}
		else {
			data.setState( State.INITIALIZED );
			data.setInstance( instance );
			final var rowProcessingState = data.getRowProcessingState();
			resolveInstanceSubInitializers( instance, rowProcessingState );
			if ( rowProcessingState.needsResolveState() ) {
				for ( var assembler : assemblers ) {
					assembler.resolveState( rowProcessingState );
				}
			}
		}
	}

	private void resolveInstanceSubInitializers(Object instance, RowProcessingState rowProcessingState) {
		for ( int i = 0; i < subInitializersForResolveFromInitialized.length; i++ ) {
			final var initializer = subInitializersForResolveFromInitialized[i];
			if ( initializer != null ) {
				final Object subInstance = representationEmbeddable.getValue( instance, i );
				if ( subInstance == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
					// Go through the normal initializer process
					initializer.resolveKey( rowProcessingState );
				}
				else {
					initializer.resolveInstance( subInstance, rowProcessingState );
				}
			}
		}
	}

	@Override
	public void initializeInstance(NonAggregatedIdentifierMappingInitializerData data) {
		if ( data.getState() == State.RESOLVED ) {
			data.setState( State.INITIALIZED );

			if ( parent != null ) {
				assert parent.isEntityInitializer();
				final Object parentInstance = parent.getResolvedInstance( data.getRowProcessingState() );
				assert parentInstance != null;
				final var lazyInitializer = HibernateProxy.extractLazyInitializer( parentInstance );
				// If the composite instance has a lazy initializer attached, this means that the embeddable is actually virtual
				// and the compositeInstance == entity, so we have to inject the row state into the entity when it finishes resolution
				if ( lazyInitializer != null ) {
					virtualIdEmbeddable.setValues( lazyInitializer.getImplementation(), data.virtualIdState );
				}
				else {
					virtualIdEmbeddable.setValues( parentInstance, data.virtualIdState );
				}
			}
		}
	}

	@Override
	protected void forEachSubInitializer(BiConsumer<Initializer<?>, RowProcessingState> consumer, InitializerData data) {
		final var rowProcessingState = data.getRowProcessingState();
		for ( var initializer : initializers ) {
			if ( initializer != null ) {
				consumer.accept( initializer, rowProcessingState );
			}
		}
	}

	private void extractRowState(NonAggregatedIdentifierMappingInitializerData data) {
		final var rowProcessingState = data.getRowProcessingState();
		for ( int i = 0; i < assemblers.length; i++ ) {
			final Object contributorValue = assemblers[i].assemble( rowProcessingState );
			if ( contributorValue == null ) {
				// This is a key and there is a null part, the whole thing has to be turned into null
				data.setState( State.MISSING );
				return;
			}
			if ( contributorValue == BATCH_PROPERTY ) {
				data.virtualIdState[i] = null;
				data.idClassState[i] = null;
			}
			else {
				data.virtualIdState[i] = contributorValue;
				data.idClassState[i] = contributorValue;
				if ( hasIdClass ) {
					final var virtualIdAttribute = virtualIdEmbeddable.getAttributeMapping( i );
					final var mappedIdAttribute = representationEmbeddable.getAttributeMapping( i );
					if ( virtualIdAttribute instanceof ToOneAttributeMapping toOneAttributeMapping
							&& !( mappedIdAttribute instanceof ToOneAttributeMapping ) ) {
						final Object associationKey =
								toOneAttributeMapping.getForeignKeyDescriptor()
										.getAssociationKeyFromSide(
												data.virtualIdState[i],
												toOneAttributeMapping.getSideNature().inverse(),
												rowProcessingState.getSession()
										);
						data.idClassState[i] = associationKey;
					}
				}
			}
		}
	}

	@Override
	public void resolveState(NonAggregatedIdentifierMappingInitializerData data) {
		if ( !data.isFindByIdLookup ) {
			final var rowProcessingState = data.getRowProcessingState();
			for ( var assembler : assemblers ) {
				assembler.resolveState( rowProcessingState );
			}
		}
	}

	@Override
	public boolean isPartOfKey() {
		return true;
	}

	@Override
	public boolean isEager() {
		// Embeddables are never lazy
		return true;
	}

	@Override
	public boolean isLazyCapable() {
		return lazyCapable;
	}

	@Override
	public boolean hasLazySubInitializers() {
		return hasLazySubInitializer;
	}

	/*
	 * Used by Hibernate Reactive
	 */
	protected @Nullable Initializer<InitializerData>[] getInitializers() {
		return initializers;
	}

	@Override
	public String toString() {
		return "NonAggregatedIdentifierMappingInitializer(" + navigablePath + ") : `"
				+ getInitializedPart().getJavaType().getJavaTypeClass() + "`";
	}
}
