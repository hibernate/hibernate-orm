/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.InitializerProducer;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.collection.CollectionResultGraphNode;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class CollectionDomainResult implements DomainResult, CollectionResultGraphNode, FetchParent,
		InitializerProducer<CollectionDomainResult> {
	private final NavigablePath loadingPath;
	private final PluralAttributeMapping loadingAttribute;

	private final String resultVariable;
	private final TableGroup tableGroup;

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
		this.tableGroup = tableGroup;
		// The collection is always the target side
		this.fkResult = loadingAttribute.getKeyDescriptor().createKeyDomainResult(
				loadingPath,
				tableGroup,
				ForeignKeyDescriptor.Nature.TARGET,
				this,
				creationState
		);

		final CollectionSemantics<?,?> collectionSemantics = loadingAttribute.getCollectionDescriptor().getCollectionSemantics();
		initializerProducer = collectionSemantics.createInitializerProducer(
				loadingPath,
				loadingAttribute,
				this,
				true,
				null,
				creationState
		);
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
	public JavaType<?> getResultJavaType() {
		return loadingAttribute.getJavaType();
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
				loadingPath,
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

}
