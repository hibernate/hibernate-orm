/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.function.Consumer;

import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.graph.collection.LoadingCollectionEntry;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class DelayedCollectionAssembler implements DomainResultAssembler {
	private final PluralAttributeMapping fetchedMapping;
	private final FetchParentAccess parentAccess;

	private final CollectionInitializer initializer;

	public DelayedCollectionAssembler(
			NavigablePath fetchPath,
			PluralAttributeMapping fetchedMapping,
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationState creationState) {
		this.fetchedMapping = fetchedMapping;
		this.parentAccess = parentAccess;
		this.initializer = new InitializerImpl( fetchPath, fetchedMapping, parentAccess, creationState );
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

		private CollectionKey collectionKey;
		private PersistentCollection instance;

		public InitializerImpl(
				NavigablePath fetchedPath,
				PluralAttributeMapping fetchedMapping,
				FetchParentAccess parentAccess,
				AssemblerCreationState creationState) {
			this.fetchedPath = fetchedPath;
			this.fetchedMapping = fetchedMapping;
			this.parentAccess = parentAccess;
		}

		@Override
		public NavigablePath getNavigablePath() {
			return fetchedPath;
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

			parentAccess.registerResolutionListener( owner -> instance.setOwner( owner ) );
		}

		@Override
		public void resolveInstance(RowProcessingState rowProcessingState) {
			final SharedSessionContractImplementor session = rowProcessingState.getSession();
			final PersistenceContext persistenceContext = session.getPersistenceContext();
			final LoadingCollectionEntry loadingEntry = persistenceContext.getLoadContexts().findLoadingCollectionEntry( collectionKey );
			final PersistentCollection registeredInstance = persistenceContext.getCollection( collectionKey );

			if ( loadingEntry != null ) {
				instance = loadingEntry.getCollectionInstance();
				return;
			}

			if ( registeredInstance != null ) {
				this.instance = registeredInstance;
				return;
			}

			this.instance = makePersistentCollection( fetchedMapping, collectionKey, rowProcessingState );
			persistenceContext.addUninitializedCollection(
					getInitializingCollectionDescriptor(),
					instance,
					collectionKey.getKey()
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
		}

		@Override
		public void finishUpRow(RowProcessingState rowProcessingState) {

			collectionKey = null;
			instance = null;
		}

		@Override
		public PluralAttributeMapping getInitializedPart() {
			return fetchedMapping;
		}

		@Override
		public PersistentCollection getCollectionInstance() {
			return instance;
		}

		@Override
		public CollectionKey resolveCollectionKey(RowProcessingState rowProcessingState) {
			resolveKey( rowProcessingState );
			return collectionKey;
		}
	}
}
