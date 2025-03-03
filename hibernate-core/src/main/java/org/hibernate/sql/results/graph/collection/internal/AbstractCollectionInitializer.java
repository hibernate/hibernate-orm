/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.function.BiConsumer;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
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
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.internal.AbstractInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.Type;

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
	protected final @Nullable Type keyTypeForEqualsHashCode;
	protected final boolean isResultInitializer;
	protected final @Nullable InitializerParent<?> parent;
	protected final @Nullable EntityInitializer<InitializerData> owningEntityInitializer;

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
		this.keyTypeForEqualsHashCode = collectionAttributeMapping.getCollectionDescriptor()
				.getKeyType()
				.getTypeForEqualsHashCode();
		this.isResultInitializer = isResultInitializer;
		this.parent = parent;
		//noinspection unchecked
		this.owningEntityInitializer = (EntityInitializer<InitializerData>) Initializer.findOwningEntityInitializer( parent );
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
	public void resolveState(Data data) {
		if ( collectionKeyResultAssembler != null ) {
			collectionKeyResultAssembler.resolveState( data.getRowProcessingState() );
		}
	}

	@Override
	public void resolveFromPreviousRow(Data data) {
		if ( data.getState() == State.UNINITIALIZED ) {
			if ( data.collectionKey == null ) {
				setMissing( data );
			}
			else {
				// A collection key can't contain collections, so no need to resolve the key
//				if ( collectionKeyResultAssembler != null ) {
//					final Initializer<?> initializer = collectionKeyResultAssembler.getInitializer();
//					if ( initializer != null ) {
//						initializer.resolveFromPreviousRow( data.getRowProcessingState() );
//					}
//				}
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
		if ( checkPreviousRow && oldKey != null && areKeysEqual( oldKey.getKey(), data.collectionKeyValue ) ) {
			data.collectionKey = oldKey;
			data.setCollectionInstance( oldCollectionInstance );
			data.setState( oldCollectionInstance == null ? State.MISSING : State.RESOLVED );
		}
		else {
			data.collectionKey = new CollectionKey( persister, data.collectionKeyValue );
			data.setState( State.KEY_RESOLVED );
		}
	}

	private boolean areKeysEqual(Object key1, Object key2) {
		return keyTypeForEqualsHashCode == null ? key1.equals( key2 ) : keyTypeForEqualsHashCode.isEqual( key1, key2 );
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
	public boolean isEager() {
		return true;
	}

	@Override
	public boolean hasLazySubInitializers() {
		return true;
	}

	@Override
	public boolean isResultInitializer() {
		return isResultInitializer;
	}
}
