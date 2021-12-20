/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.collection.CollectionResultGraphNode;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class CollectionDomainResult implements DomainResult, CollectionResultGraphNode, FetchParent {
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

		fkResult = loadingAttribute.getKeyDescriptor().createKeyDomainResult(
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
	public JavaType<?> getResultJavaTypeDescriptor() {
		return loadingAttribute.getJavaTypeDescriptor();
	}

	@Override
	public DomainResultAssembler createResultAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		final CollectionInitializer initializer = (CollectionInitializer) creationState.resolveInitializer(
				getNavigablePath(),
				getReferencedModePart(),
				() -> {
					final DomainResultAssembler fkAssembler = fkResult.createResultAssembler( null, creationState );

					return initializerProducer.produceInitializer(
							loadingPath,
							loadingAttribute,
							null,
							LockMode.READ,
							fkAssembler,
							fkAssembler,
							creationState
					);
				}
		);

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
		return Collections.emptyList();
	}

	@Override
	public Fetch findFetch(Fetchable fetchable) {
		return null;
	}

}
