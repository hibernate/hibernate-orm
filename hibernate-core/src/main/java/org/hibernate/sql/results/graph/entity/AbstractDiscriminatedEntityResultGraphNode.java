/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity;

import java.util.BitSet;

import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.DomainResultGraphNode;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.type.descriptor.java.JavaType;

public abstract class AbstractDiscriminatedEntityResultGraphNode implements DomainResultGraphNode, FetchParent {
	private final NavigablePath navigablePath;

	private final DiscriminatedAssociationModelPart graphedPart;
	private final JavaType<?> baseAssociationJtd;

	private Fetch discriminatorValueFetch;
	private Fetch keyValueFetch;
	private ImmutableFetchList fetches;

	public AbstractDiscriminatedEntityResultGraphNode(
			NavigablePath navigablePath,
			DiscriminatedAssociationModelPart graphedPart,
			JavaType<?> baseAssociationJtd) {
		this.navigablePath = navigablePath;
		this.graphedPart = graphedPart;
		this.baseAssociationJtd = baseAssociationJtd;
	}

	protected void afterInitialize(DomainResultCreationState creationState) {
		this.fetches = creationState.visitFetches( this );
		assert fetches.size() == 2;

		discriminatorValueFetch = fetches.get( graphedPart.getDiscriminatorPart() );
		keyValueFetch = fetches.get( graphedPart.getKeyPart() );
	}

	public Fetch getDiscriminatorValueFetch() {
		return discriminatorValueFetch;
	}

	public Fetch getKeyValueFetch() {
		return keyValueFetch;
	}

	public JavaType<?> getBaseAssociationJtd() {
		return baseAssociationJtd;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return baseAssociationJtd;
	}

	@Override
	public boolean containsAnyNonScalarResults() {
		return true;
	}

	@Override
	public DiscriminatedAssociationModelPart getReferencedMappingContainer() {
		return graphedPart;
	}

	@Override
	public DiscriminatedAssociationModelPart getReferencedMappingType() {
		return graphedPart;
	}

	@Override
	public ImmutableFetchList getFetches() {
		return fetches;
	}

	@Override
	public Fetch findFetch(Fetchable fetchable) {
		if ( graphedPart.getDiscriminatorPart() == fetchable ) {
			return discriminatorValueFetch;
		}

		if ( graphedPart.getKeyPart() == fetchable ) {
			return keyValueFetch;
		}

		throw new IllegalArgumentException( "Given Fetchable [" + fetchable + "] did not match either discriminator nor key mapping" );
	}

	@Override
	public boolean hasJoinFetches() {
		return false;
	}

	@Override
	public boolean containsCollectionFetches() {
		return false;
	}

	public void collectValueIndexesToCache(BitSet valueIndexes) {
		FetchParent.super.collectValueIndexesToCache( valueIndexes );
	}
}
