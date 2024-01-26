/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerProducer;
import org.hibernate.sql.results.graph.entity.AbstractDiscriminatedEntityResultGraphNode;
import org.hibernate.type.descriptor.java.JavaType;

public class DiscriminatedEntityFetch extends AbstractDiscriminatedEntityResultGraphNode implements Fetch,
		InitializerProducer<DiscriminatedEntityFetch> {
	private final FetchTiming fetchTiming;
	private final FetchParent fetchParent;

	public DiscriminatedEntityFetch(
			NavigablePath navigablePath,
			JavaType<?> baseAssociationJtd,
			DiscriminatedAssociationModelPart fetchedPart,
			FetchTiming fetchTiming,
			FetchParent fetchParent,
			DomainResultCreationState creationState) {
		super( navigablePath, fetchedPart, baseAssociationJtd );
		this.fetchTiming = fetchTiming;
		this.fetchParent = fetchParent;

		afterInitialize( creationState );
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public DiscriminatedAssociationModelPart getFetchedMapping() {
		return getReferencedMappingContainer();
	}

	@Override
	public FetchTiming getTiming() {
		return fetchTiming;
	}

	@Override
	public boolean hasTableGroup() {
		return false;
	}

	@Override
	public DomainResultAssembler<?> createAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		return new EntityAssembler(
				getReferencedMappingContainer().getJavaType(),
				creationState.resolveInitializer( this, parentAccess, this ).asEntityInitializer()
		);
	}

	@Override
	public Initializer createInitializer(
			DiscriminatedEntityFetch resultGraphNode,
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		return resultGraphNode.createInitializer( parentAccess, creationState );
	}

	@Override
	public Initializer createInitializer(FetchParentAccess parentAccess, AssemblerCreationState creationState) {
		return new DiscriminatedEntityInitializer(
				parentAccess,
				getReferencedMappingType(),
				getNavigablePath(),
				getDiscriminatorValueFetch(),
				getKeyValueFetch(),
				fetchTiming == FetchTiming.IMMEDIATE,
				false,
				creationState
		);
	}

	@Override
	public FetchParent asFetchParent() {
		return this;
	}
}
