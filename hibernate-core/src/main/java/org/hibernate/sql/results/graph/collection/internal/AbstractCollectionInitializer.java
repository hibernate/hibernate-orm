/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.function.BiConsumer;

import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.collection.LoadingCollectionEntry;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.internal.AbstractInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Base support for CollectionInitializer implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionInitializer<Data extends AbstractCollectionInitializer.CollectionInitializerData>
		extends AbstractInitializer<Data> implements CollectionInitializer<Data> {
	protected final NavigablePath collectionPath;
	protected final PluralAttributeMapping collectionAttributeMapping;
	protected final boolean isResultInitializer;
	protected final @Nullable InitializerParent<?> parent;
	protected final @Nullable EntityInitializer<?> owningEntityInitializer;

	/**
	 * refers to the collection's container value - which collection-key?
	 */
	protected final @Nullable DomainResultAssembler<?> collectionKeyResultAssembler;

	public static class CollectionInitializerData extends InitializerData {
		// per-row state
		protected @Nullable Object collectionKeyValue;
		protected @Nullable CollectionKey collectionKey;

		public CollectionInitializerData(RowProcessingState rowProcessingState) {
			super( rowProcessingState );
		}

		public @Nullable PersistentCollection<?> getCollectionInstance() {
			return (PersistentCollection<?>) getInstance();
		}

		public void setCollectionInstance(@Nullable PersistentCollection<?> collectionInstance) {
			setInstance( collectionInstance );
		}
	}

	protected AbstractCollectionInitializer(
			NavigablePath collectionPath,
			PluralAttributeMapping collectionAttributeMapping,
			InitializerParent<?> parent,
			@Nullable DomainResult<?> collectionKeyResult,
			boolean isResultInitializer,
			AssemblerCreationState creationState) {
		super( creationState );
		this.collectionPath = collectionPath;
		this.collectionAttributeMapping = collectionAttributeMapping;
		this.isResultInitializer = isResultInitializer;
		this.parent = parent;
		this.owningEntityInitializer = Initializer.findOwningEntityInitializer( parent );
		this.collectionKeyResultAssembler = collectionKeyResult == null
				? null
				: collectionKeyResult.createResultAssembler( this, creationState );
	}

	@Override
	protected InitializerData createInitializerData(RowProcessingState rowProcessingState) {
		return new CollectionInitializerData( rowProcessingState );
	}

	@Override
	public void resolveKey(Data data) {
		if ( data.getState() != State.UNINITIALIZED ) {
			// already resolved
			return;
		}
		data.setState( State.KEY_RESOLVED );
		data.collectionKeyValue = null;
		if ( collectionKeyResultAssembler != null ) {
			//noinspection unchecked
			final Initializer<InitializerData> initializer = (Initializer<InitializerData>) collectionKeyResultAssembler.getInitializer();
			final RowProcessingState rowProcessingState = data.getRowProcessingState();
			if ( initializer != null ) {
				final InitializerData subData = initializer.getData( rowProcessingState );
				initializer.resolveKey( subData );
				if ( subData.getState() == State.MISSING ) {
					setMissing( data );
				}
				return;
			}
			data.collectionKeyValue = collectionKeyResultAssembler.assemble( rowProcessingState );
			if ( data.collectionKeyValue == null ) {
				setMissing( data );
			}
		}
	}

	@Override
	public void resolveFromPreviousRow(Data data) {
		if ( data.getState() == State.UNINITIALIZED ) {
			if ( data.collectionKey == null ) {
				setMissing( data );
			}
			else {
				if ( collectionKeyResultAssembler != null ) {
					final Initializer<?> initializer = collectionKeyResultAssembler.getInitializer();
					if ( initializer != null ) {
						initializer.resolveFromPreviousRow( data.getRowProcessingState() );
					}
				}
				data.setState( State.RESOLVED );
			}
		}
	}

	protected void setMissing(Data data) {
		data.setState( State.MISSING );
		data.collectionKey = null;
		data.collectionKeyValue = null;
		data.setCollectionInstance( null );
	}

	protected void resolveCollectionKey(Data data, boolean checkPreviousRow) {
		final CollectionKey oldKey = data.collectionKey;
		final PersistentCollection<?> oldCollectionInstance = data.getCollectionInstance();
		data.collectionKey = null;
		data.setCollectionInstance( null );

		if ( data.collectionKeyValue == null ) {
			if ( collectionKeyResultAssembler == null ) {
				assert owningEntityInitializer != null;
				data.collectionKeyValue = owningEntityInitializer.getEntityIdentifier( data.getRowProcessingState() );
			}
			else {
				data.collectionKeyValue = collectionKeyResultAssembler.assemble( data.getRowProcessingState() );
			}
			if ( data.collectionKeyValue == null ) {
				data.setState( State.MISSING );
				data.collectionKey = null;
				data.setCollectionInstance( null );
				return;
			}
		}
		final CollectionPersister persister = collectionAttributeMapping.getCollectionDescriptor();
		// Try to reuse the previous collection key and collection if possible
		if ( checkPreviousRow && oldKey != null && persister.getKeyType().isEqual(
				oldKey.getKey(),
				data.collectionKeyValue
		) ) {
			data.collectionKey = oldKey;
			data.setCollectionInstance( oldCollectionInstance );
			data.setState( oldCollectionInstance == null ? State.MISSING : State.RESOLVED );
		}
		else {
			data.collectionKey = new CollectionKey( persister, data.collectionKeyValue );
			data.setState( State.KEY_RESOLVED );
		}
	}

	protected void resolveInstance(Data data, boolean isEager) {
		if ( data.getState() != State.KEY_RESOLVED ) {
			// already resolved
			return;
		}

		resolveCollectionKey( data, false );
		if ( data.getState() == State.KEY_RESOLVED ) {
			assert parent != null;
			final RowProcessingState rowProcessingState = data.getRowProcessingState();
			// We can avoid processing further if the parent is already initialized,
			// as the value produced by this initializer will never be used anyway.
			if ( owningEntityInitializer != null
					&& owningEntityInitializer.getData( rowProcessingState ).getState() == State.INITIALIZED ) {
				// It doesn't matter if it's eager or lazy, the collection object can not be referred to,
				// so it doesn't make sense to create or initialize it
				data.setState( State.MISSING );
				return;
			}
			data.setState( State.RESOLVED );

			final SharedSessionContractImplementor session = rowProcessingState.getSession();
			final PersistenceContext persistenceContext = session.getPersistenceContext();

			final LoadingCollectionEntry loadingEntry = persistenceContext.getLoadContexts()
					.findLoadingCollectionEntry( data.collectionKey );

			if ( loadingEntry != null ) {
				data.setCollectionInstance( loadingEntry.getCollectionInstance() );
				if ( data.getCollectionInstance().getOwner() == null ) {
					assert owningEntityInitializer.getTargetInstance( rowProcessingState ) != null;
					data.getCollectionInstance().setOwner( owningEntityInitializer.getTargetInstance( rowProcessingState ) );
				}
				return;
			}

			final PersistentCollection<?> existing = persistenceContext.getCollection( data.collectionKey );

			if ( existing != null ) {
				data.setCollectionInstance( existing );
				if ( data.getCollectionInstance().getOwner() == null ) {
					assert owningEntityInitializer.getTargetInstance( rowProcessingState ) != null;
					data.getCollectionInstance().setOwner( owningEntityInitializer.getTargetInstance( rowProcessingState ) );
				}
				return;
			}

			final CollectionPersister collectionDescriptor = collectionAttributeMapping.getCollectionDescriptor();
			final CollectionSemantics<?, ?> collectionSemantics = collectionDescriptor.getCollectionSemantics();
			final Object key = data.collectionKey.getKey();

			data.setCollectionInstance( collectionSemantics.instantiateWrapper(
					key,
					collectionDescriptor,
					session
			) );

			assert owningEntityInitializer.getTargetInstance( rowProcessingState ) != null;
			data.getCollectionInstance().setOwner( owningEntityInitializer.getTargetInstance( rowProcessingState ) );

			persistenceContext.addUninitializedCollection(
					collectionDescriptor,
					data.getCollectionInstance(),
					key
			);

			if ( isEager ) {
				persistenceContext.addNonLazyCollection( data.getCollectionInstance() );
			}

			if ( collectionSemantics.getCollectionClassification() == CollectionClassification.ARRAY ) {
				session.getPersistenceContext().addCollectionHolder( data.getCollectionInstance() );
			}
		}
	}

	protected void resolveInstance(Object instance, Data data, boolean isEager) {
		if ( instance == null ) {
			setMissing( data );
		}
		else {
			final RowProcessingState rowProcessingState = data.getRowProcessingState();
			final PersistenceContext persistenceContext = rowProcessingState.getSession().getPersistenceContextInternal();
			final PersistentCollection<?> persistentCollection;
			if ( collectionAttributeMapping.getCollectionDescriptor()
					.getCollectionSemantics()
					.getCollectionClassification() == CollectionClassification.ARRAY ) {
				persistentCollection = persistenceContext.getCollectionHolder( instance );
			}
			else {
				persistentCollection = (PersistentCollection<?>) instance;
			}
			// resolving the collection key seems unnecessary
//			collectionKeyValue = persistentCollection.getKey();
//			resolveCollectionKey( rowProcessingState, false );
			data.setCollectionInstance( persistentCollection );
			data.setState( State.RESOLVED );
			if ( isEager && !data.getCollectionInstance().wasInitialized() ) {
				persistenceContext.addNonLazyCollection( data.getCollectionInstance() );
			}
			if ( collectionKeyResultAssembler != null && rowProcessingState.needsResolveState() ) {
				// Resolve the state of the identifier if result caching is enabled and this is not a query cache hit
				collectionKeyResultAssembler.resolveState( rowProcessingState );
			}
		}
	}

	@Override
	protected void forEachSubInitializer(BiConsumer<Initializer<?>, RowProcessingState> consumer, InitializerData data) {
		if ( collectionKeyResultAssembler != null ) {
			final Initializer<?> initializer = collectionKeyResultAssembler.getInitializer();
			if ( initializer != null ) {
				consumer.accept( initializer, data.getRowProcessingState() );
			}
		}
	}

	@Override
	public @Nullable PersistentCollection<?> getCollectionInstance(Data data) {
		return data.getState() == State.UNINITIALIZED || data.getState() == State.MISSING ? null :
				data.getCollectionInstance();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return collectionPath;
	}

	public PluralAttributeMapping getCollectionAttributeMapping() {
		return collectionAttributeMapping;
	}

	@Override
	public PluralAttributeMapping getInitializedPart() {
		return getCollectionAttributeMapping();
	}

	@Override
	public @Nullable InitializerParent<?> getParent() {
		return parent;
	}

	@Override
	public boolean isPartOfKey() {
		// A collection can never be part of a key
		return false;
	}

	@Override
	public boolean isResultInitializer() {
		return isResultInitializer;
	}
}
