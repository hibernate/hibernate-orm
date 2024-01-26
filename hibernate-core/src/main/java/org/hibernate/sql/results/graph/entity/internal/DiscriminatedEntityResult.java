/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerProducer;
import org.hibernate.sql.results.graph.entity.AbstractDiscriminatedEntityResultGraphNode;
import org.hibernate.type.descriptor.java.JavaType;

public class DiscriminatedEntityResult<T> extends AbstractDiscriminatedEntityResultGraphNode implements DomainResult<T>,
		InitializerProducer<DiscriminatedEntityResult<T>> {
	private final String resultVariable;

	public DiscriminatedEntityResult(
			NavigablePath navigablePath,
			JavaType<?> baseAssociationJtd,
			DiscriminatedAssociationModelPart fetchedPart,
			String resultVariable,
			DomainResultCreationState creationState) {
		super( navigablePath, fetchedPart, baseAssociationJtd );
		this.resultVariable = resultVariable;

		afterInitialize( creationState );
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public DomainResultAssembler<T> createResultAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		//noinspection unchecked
		return new EntityAssembler(
				getReferencedMappingContainer().getJavaType(),
				creationState.resolveInitializer( this, parentAccess, this ).asEntityInitializer()
		);
	}

	@Override
	public Initializer createInitializer(
			DiscriminatedEntityResult<T> resultGraphNode,
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
				true,
				true,
				creationState
		);
	}
}
