/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.collection;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.model.domain.spi.CollectionKey;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.CollectionFetch;
import org.hibernate.sql.results.spi.CollectionInitializer;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Initializer;

/**
 * An {@link FetchTiming#IMMEDIATE} collection fetch
 *
 * @author Steve Ebersole
 */
public class CollectionFetchImpl extends AbstractCollectionMappingNode implements CollectionFetch {
	public static CollectionFetchImpl create(
			NavigablePath navigablePath,
			TableGroup containerTableGroup,
			TableGroup collectionTableGroup,
			FetchParent fetchParent,
			PluralPersistentAttribute describedAttribute,
			String resultVariable,
			LockMode lockMode,
			CollectionInitializerProducer initializerProducer,
			DomainResultCreationState creationState) {
		DomainResult keyContainerResult;
		DomainResult keyCollectionResult = null;

		if ( fetchParent == null ) {
			throw new IllegalStateException( "FetchParent cannot be null when creating a CollectionFetch" );
		}

		if ( collectionTableGroup != null ) {
			// join fetch
			final CollectionKey collectionKey = describedAttribute.getPersistentCollectionDescriptor().getCollectionKeyDescriptor();

			keyContainerResult = collectionKey.createContainerResult( containerTableGroup, creationState );
			keyCollectionResult = collectionKey.createCollectionResult( collectionTableGroup, creationState );
		}
		else {
			// select fetch
			// todo (6.0) : we could potentially leverage batch fetching for performance
			keyContainerResult = describedAttribute.getPersistentCollectionDescriptor()
					.getCollectionKeyDescriptor()
					.createContainerResult( containerTableGroup, creationState );
			// use null for `keyCollectionResult`... the initializer will see that as trigger to use
			// the assembled container-key value as the collection-key value.
		}

		return new CollectionFetchImpl(
				navigablePath,
				fetchParent,
				describedAttribute,
				resultVariable,
				lockMode,
				keyContainerResult,
				keyCollectionResult,
				initializerProducer
		);
	}

	private final NavigablePath navigablePath;
	private final LockMode lockMode;
	private final CollectionInitializerProducer initializerProducer;

	public CollectionFetchImpl(
			NavigablePath navigablePath,
			FetchParent fetchParent,
			PluralPersistentAttribute describedAttribute,
			String resultVariable,
			LockMode lockMode,
			DomainResult keyContainerResult,
			DomainResult keyCollectionResult,
			CollectionInitializerProducer initializerProducer) {
		super( fetchParent, describedAttribute, resultVariable, keyContainerResult, keyCollectionResult );
		this.navigablePath = navigablePath;
		this.lockMode = lockMode;
		this.initializerProducer = initializerProducer;
	}

	@Override
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationState creationState) {
		final DomainResultAssembler keyContainerAssembler = getKeyContainerResult().createResultAssembler(
				collector,
				creationState
		);

		final DomainResultAssembler keyCollectionAssembler;
		if ( getKeyCollectionResult() == null ) {
			keyCollectionAssembler = null;
		}
		else {
			keyCollectionAssembler = getKeyCollectionResult().createResultAssembler(
					collector,
					creationState
			);
		}

		final CollectionInitializer initializer = initializerProducer.produceInitializer(
				parentAccess,
				getNavigablePath(),
				lockMode,
				keyContainerAssembler,
				keyCollectionAssembler,
				collector,
				creationState
		);

		collector.accept( initializer );

		return new PluralAttributeAssemblerImpl( initializer );
	}

	@Override
	public FetchParent getFetchParent() {
		return super.getFetchParent();
	}

	@Override
	public PluralPersistentAttribute getFetchedNavigable() {
		return getPluralAttribute();
	}

	@Override
	public boolean isNullable() {
		return true;
	}
}
