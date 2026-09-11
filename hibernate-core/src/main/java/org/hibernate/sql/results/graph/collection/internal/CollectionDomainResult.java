/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.LockMode;
import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.InitializerProducer;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.collection.CollectionResultGraphNode;

import java.util.BitSet;

/**
 * @author Steve Ebersole
 */
public class CollectionDomainResult extends AbstractFetchParent implements DomainResult, CollectionResultGraphNode,
		InitializerProducer<CollectionDomainResult> {
	private final PluralAttributeMapping loadingAttribute;

	private final String resultVariable;
	private final TableGroup tableGroup;

	private final DomainResult fkResult;

	private final @Nullable Fetch identifierFetch;

	private final CollectionInitializerProducer initializerProducer;

	public CollectionDomainResult(
			NavigablePath loadingPath,
			PluralAttributeMapping loadingAttribute,
			String resultVariable,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		super( loadingPath );
		this.loadingAttribute = loadingAttribute;
		this.resultVariable = resultVariable;
		this.tableGroup = tableGroup;
		// The collection is always the target side
		this.fkResult = loadingAttribute.getKeyDescriptor().createKeyDomainResult(
				loadingPath,
				tableGroup,
				ForeignKeyDescriptor.Nature.TARGET,
				this,
				creationState
		);

		if ( loadingAttribute.getIdentifierDescriptor() != null ) {
			identifierFetch = generateFetchableFetch(
					loadingAttribute.getIdentifierDescriptor(),
					loadingPath.append( CollectionPart.Nature.ID.getName() ),
					FetchTiming.IMMEDIATE,
					true,
					null,
					creationState
			);
		}
		else {
			identifierFetch = null;
		}

		resetFetches( creationState.visitFetches( this ) );

		final Fetch indexFetch;
		final Fetch elementFetch;
		if ( loadingAttribute.getIndexDescriptor() != null ) {
			assert getFetches().size() == 2;
			indexFetch = getFetches().get( loadingAttribute.getIndexDescriptor() );
			elementFetch = getFetches().get( loadingAttribute.getElementDescriptor() );
		}
		else {
			if ( !getFetches().isEmpty() ) { // might be empty due to fetch depth limit
				assert getFetches().size() == 1;
				indexFetch = null;
				elementFetch = getFetches().get( loadingAttribute.getElementDescriptor() );
			}
			else {
				indexFetch = null;
				elementFetch = null;
			}
		}

		final CollectionSemantics<?,?> collectionSemantics = loadingAttribute.getCollectionDescriptor().getCollectionSemantics();
		initializerProducer = collectionSemantics.createInitializerProducer(
				loadingAttribute,
				identifierFetch,
				indexFetch,
				elementFetch
		);
	}

	@Override
	public void afterInitialize(FetchParent fetchParent, DomainResultCreationState creationState) {
		// No-op
	}

	@Override
	public FetchableContainer getFetchContainer() {
		return loadingAttribute;
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public boolean containsAnyNonScalarResults() {
		return true;
	}

	@Override
	public DomainResultAssembler createResultAssembler(
			InitializerParent parent,
			AssemblerCreationState creationState) {
		return new CollectionAssembler( loadingAttribute, creationState.resolveInitializer( this, parent, this ).asCollectionInitializer() );
	}

	@Override
	public CollectionInitializer<?> createInitializer(
			CollectionDomainResult resultGraphNode,
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		return resultGraphNode.createInitializer( parent, creationState );
	}

	@Override
	public CollectionInitializer<?> createInitializer(InitializerParent<?> parent, AssemblerCreationState creationState) {
		return initializerProducer.produceInitializer(
				getNavigablePath(),
				loadingAttribute,
				parent,
				LockMode.READ,
				fkResult,
				fkResult,
				true,
				creationState
		);
	}

	@Override
	public FetchableContainer getReferencedMappingType() {
		return getReferencedMappingContainer();
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {
		if ( identifierFetch != null ) {
			identifierFetch.collectValueIndexesToCache( valueIndexes );
		}
		if ( !loadingAttribute.getCollectionDescriptor().useShallowQueryCacheLayout() ) {
			fkResult.collectValueIndexesToCache( valueIndexes );
			super.collectValueIndexesToCache( valueIndexes );
		}
	}

}
