/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.collection;

import java.util.function.Consumer;

import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.internal.LoadingCollectionEntryImpl;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.CollectionInitializer;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.LoadContexts;
import org.hibernate.sql.results.spi.LoadingCollectionEntry;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class EagerCollectionAssembler implements DomainResultAssembler {
	private final PluralAttributeMapping fetchedMapping;
	private final FetchParentAccess parentAccess;

	private final CollectionInitializer initializer;

	public EagerCollectionAssembler(
			NavigablePath fetchPath,
			PluralAttributeMapping fetchedMapping,
			DomainResult fkResult,
			Fetch elementFetch,
			Fetch indexFetch,
			DomainResult identifierResult,
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationState creationState) {
		this.fetchedMapping = fetchedMapping;
		this.parentAccess = parentAccess;

		this.initializer = new InitializerImpl(
				fetchPath,
				fetchedMapping,
				parentAccess,
				fkResult,
				elementFetch,
				indexFetch,
				identifierResult,
				collector,
				creationState
		);

		collector.accept( initializer );
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
		return initializer.getCollectionInstance();
	}

	@Override
	public JavaTypeDescriptor getAssembledJavaTypeDescriptor() {
		return fetchedMapping.getJavaTypeDescriptor();
	}

	private static class InitializerImpl implements CollectionInitializer {
		private final NavigablePath fetchedPath;
		private final PluralAttributeMapping fetchedMapping;
		private final FetchParentAccess parentAccess;

		private final DomainResultAssembler fkAssembler;
		private final DomainResultAssembler elementAssembler;
		private final DomainResultAssembler indexAssembler;
		private final DomainResultAssembler identifierAssembler;

		private CollectionKey collectionKey;

		private boolean managing;
		private Object fkValue;

		// todo (6.0) : consider using the initializer itself as the holder of the various "temp" collections
		//  		used while reading a collection.  that would mean collection-type specific initializers (List, versus Set)
		private PersistentCollection instance;

		public InitializerImpl(
				NavigablePath fetchedPath,
				PluralAttributeMapping fetchedMapping,
				FetchParentAccess parentAccess,
				DomainResult fkResult,
				Fetch elementFetch,
				Fetch indexFetch,
				DomainResult identifierResult,
				Consumer<Initializer> collector,
				AssemblerCreationState creationState) {
			this.fetchedPath = fetchedPath;
			this.fetchedMapping = fetchedMapping;
			this.parentAccess = parentAccess;

			this.fkAssembler = fkResult.createResultAssembler( collector, creationState );

			// questionable what should be the parent access here
			this.elementAssembler = elementFetch.createAssembler( parentAccess, collector, creationState );
			this.indexAssembler = indexFetch == null
					? null
					: indexFetch.createAssembler( parentAccess, collector, creationState );
			this.identifierAssembler = identifierResult == null
					? null
					: identifierResult.createResultAssembler( collector, creationState );
		}

		@Override
		public CollectionPersister getInitializingCollectionDescriptor() {
			return fetchedMapping.getCollectionDescriptor();
		}

		@Override
		public PersistentCollection getCollectionInstance() {
			return instance;
		}

		@Override
		public NavigablePath getNavigablePath() {
			return fetchedPath;
		}

		@Override
		public void finishUpRow(RowProcessingState rowProcessingState) {
			collectionKey = null;
			managing = false;
			instance = null;
		}

		@Override
		public void resolveKey(RowProcessingState rowProcessingState) {
			if ( collectionKey != null ) {
				// already resolved
				return;
			}

			collectionKey = new CollectionKey(
					fetchedMapping.getCollectionDescriptor(),
					parentAccess.getParentKey()
			);

			final Object fkValue = fkAssembler.assemble( rowProcessingState );
			if ( fkValue == null ) {
				// this row has no collection element
				return;
			}
		}

		@Override
		public void resolveInstance(RowProcessingState rowProcessingState) {
			if ( instance != null ) {
				// already resolved
				return;
			}

			final PersistenceContext persistenceContext = rowProcessingState.getSession().getPersistenceContext();

			// see if the collection is already being initialized

			final LoadContexts loadContexts = persistenceContext.getLoadContexts();
			final LoadingCollectionEntry existingEntry = loadContexts.findLoadingCollectionEntry( collectionKey );

			if ( existingEntry != null ) {
				this.instance = existingEntry.getCollectionInstance();
				if ( existingEntry.getInitializer() == this ) {
					this.managing = true;
				}
				return;
			}

			// see if it has been already registered with the Session

			final PersistentCollection registeredInstance = persistenceContext.getCollection( collectionKey );
			if ( registeredInstance != null ) {
				this.instance = registeredInstance;
				// it was already registered, so use that wrapper.
				if ( ! registeredInstance.wasInitialized() ) {
					// if the existing wrapper is not initialized, we will take responsibility for initializing it
					managing = true;
					rowProcessingState.getJdbcValuesSourceProcessingState().registerLoadingCollection(
							collectionKey,
							new LoadingCollectionEntryImpl(
									getInitializingCollectionDescriptor(),
									this,
									collectionKey,
									registeredInstance
							)
					);
					return;
				}
			}

			this.instance = makePersistentCollection( fetchedMapping, collectionKey, rowProcessingState );
			this.managing = true;

			rowProcessingState.getJdbcValuesSourceProcessingState().registerLoadingCollection(
					collectionKey,
					new LoadingCollectionEntryImpl(
							fetchedMapping.getCollectionDescriptor(),
							this,
							collectionKey.getKey(),
							instance
					)
			);

		}

		private static PersistentCollection makePersistentCollection(
				PluralAttributeMapping fetchedMapping,
				CollectionKey collectionKey,
				RowProcessingState rowProcessingState) {
			final CollectionPersister collectionDescriptor = fetchedMapping.getCollectionDescriptor();
			final CollectionSemantics collectionSemantics = collectionDescriptor.getCollectionSemantics();
			return collectionSemantics.instantiateWrapper(
					collectionKey.getKey(),
					collectionDescriptor,
					rowProcessingState.getSession()
			);
		}

		@Override
		public void initializeInstance(RowProcessingState rowProcessingState) {
			if ( ! managing ) {
				return;
			}

			final Object fkValue = fkAssembler.assemble( rowProcessingState );
			if ( fkValue == null ) {
				// this row contains no collection element
				return;
			}

			getCollectionInstance().readFrom(
					rowProcessingState,
					elementAssembler,
					indexAssembler,
					identifierAssembler,
					parentAccess.getFetchParentInstance()
			);
		}
	}
}
