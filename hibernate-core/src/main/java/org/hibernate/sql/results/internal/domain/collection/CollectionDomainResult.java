/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.collection;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.internal.LoadingCollectionEntryImpl;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.CollectionInitializer;
import org.hibernate.sql.results.spi.CollectionResultNode;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.FetchableContainer;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.LoadContexts;
import org.hibernate.sql.results.spi.LoadingCollectionEntry;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class CollectionDomainResult implements DomainResult, CollectionResultNode, FetchParent {
	private final NavigablePath loadingPath;
	private final PluralAttributeMapping loadingAttribute;

	private final String resultVariable;

	private final DomainResult fkResult;

	private final CollectionInitializerProducer initializerProducer;

	public CollectionDomainResult(
			NavigablePath loadingPath,
			PluralAttributeMapping loadingAttribute,
			String resultVariable,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		this.loadingPath = loadingPath;
		this.loadingAttribute = loadingAttribute;
		this.resultVariable = resultVariable;

		fkResult = loadingAttribute.getKeyDescriptor().createDomainResult(
				loadingPath,
				tableGroup,
				creationState
		);

		final CollectionSemantics collectionSemantics = loadingAttribute.getCollectionDescriptor().getCollectionSemantics();
		initializerProducer = collectionSemantics.createInitializerProducer(
				loadingPath,
				loadingAttribute,
				this,
				true,
				null,
				LockMode.READ,
				creationState
		);
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public JavaTypeDescriptor getResultJavaTypeDescriptor() {
		return loadingAttribute.getJavaTypeDescriptor();
	}

	@Override
	public DomainResultAssembler createResultAssembler(
			Consumer initializerCollector,
			AssemblerCreationState creationState) {

		final DomainResultAssembler fkAssembler = fkResult.createResultAssembler(
				initializerCollector,
				creationState
		);

		final CollectionInitializer initializer = initializerProducer.produceInitializer(
				loadingPath,
				loadingAttribute,
				null,
				LockMode.READ,
				fkAssembler,
				fkAssembler,
				initializerCollector,
				creationState
		);

		initializerCollector.accept( initializer );

		return new EagerCollectionAssembler( loadingAttribute, initializer );
	}

	@Override
	public FetchableContainer getReferencedMappingContainer() {
		return loadingAttribute;
	}

	@Override
	public FetchableContainer getReferencedMappingType() {
		return getReferencedMappingContainer();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return loadingPath;
	}

	@Override
	public List<Fetch> getFetches() {
		return null;
	}

	@Override
	public Fetch findFetch(String fetchableName) {
		return null;
	}


	private static class InitializerImpl implements CollectionInitializer {
		private final NavigablePath loadingPath;
		private final PluralAttributeMapping loadingCollection;

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
				NavigablePath loadingPath,
				PluralAttributeMapping loadingCollection,
				DomainResult fkResult,
				Fetch elementFetch,
				Fetch indexFetch,
				DomainResult identifierResult,
				Consumer<Initializer> collector,
				AssemblerCreationState creationState) {
			this.loadingPath = loadingPath;
			this.loadingCollection = loadingCollection;

			this.fkAssembler = fkResult.createResultAssembler( collector, creationState );

			// questionable what should be the parent access here
			this.elementAssembler = elementFetch.createAssembler( null, collector, creationState );
			this.indexAssembler = indexFetch == null
					? null
					: indexFetch.createAssembler( null, collector, creationState );
			this.identifierAssembler = identifierResult == null
					? null
					: identifierResult.createResultAssembler( collector, creationState );
		}

		@Override
		public PluralAttributeMapping getInitializedPart() {
			return loadingCollection;
		}

		@Override
		public PersistentCollection getCollectionInstance() {
			return instance;
		}

		@Override
		public NavigablePath getNavigablePath() {
			return loadingPath;
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

			final Object fkValue = fkAssembler.assemble( rowProcessingState );
			assert fkValue != null;

			collectionKey = new CollectionKey(
					loadingCollection.getCollectionDescriptor(),
					fkValue
			);
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

			this.instance = makePersistentCollection( loadingCollection, collectionKey, rowProcessingState );
			this.managing = true;

			rowProcessingState.getJdbcValuesSourceProcessingState().registerLoadingCollection(
					collectionKey,
					new LoadingCollectionEntryImpl(
							loadingCollection.getCollectionDescriptor(),
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

			final PersistentCollection collectionInstance = getCollectionInstance();
			collectionInstance.readFrom(
					rowProcessingState,
					elementAssembler,
					indexAssembler,
					identifierAssembler,
					collectionInstance.getOwner()
			);
		}
	}
}
