/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.InitializerProducer;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;

/**
 * @author Steve Ebersole
 */
public abstract class CollectionFetch implements FetchParent, Fetch, InitializerProducer<CollectionFetch> {
	private final NavigablePath fetchedPath;
	private final PluralAttributeMapping fetchedAttribute;

	private final FetchParent fetchParent;

	public CollectionFetch(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			FetchParent fetchParent) {
		this.fetchedPath = fetchedPath;
		this.fetchedAttribute = fetchedAttribute;
		this.fetchParent = fetchParent;
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public PluralAttributeMapping getFetchedMapping() {
		return fetchedAttribute;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return fetchedPath;
	}

	@Override
	public PluralAttributeMapping getReferencedMappingContainer() {
		return getFetchedMapping();
	}

	@Override
	public PluralAttributeMapping getReferencedMappingType() {
		return getFetchedMapping();
	}

	@Override
	public ImmutableFetchList getFetches() {
		return ImmutableFetchList.EMPTY;
	}

	@Override
	public Fetch findFetch(Fetchable fetchable) {
		return null;
	}

	@Override
	public boolean hasJoinFetches() {
		return false;
	}

	@Override
	public boolean containsCollectionFetches() {
		return false;
	}

	@Override
	public DomainResultAssembler<?> createAssembler(
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		return new CollectionAssembler(
				getFetchedMapping(),
				creationState.resolveInitializer( this, parent, this ).asCollectionInitializer()
		);
	}

	@Override
	public Initializer<?> createInitializer(
			CollectionFetch resultGraphNode,
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		return resultGraphNode.createInitializer( parent, creationState );
	}
}
