/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.composite;

import java.util.function.Consumer;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.internal.domain.AbstractFetchParent;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.CompositeResultMappingNode;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Fetchable;
import org.hibernate.sql.results.spi.Initializer;

/**
 * @author Steve Ebersole
 */
public class CompositeFetch extends AbstractFetchParent implements CompositeResultMappingNode, Fetch {
	private final FetchParent fetchParent;
	private final FetchTiming fetchTiming;
	private final boolean nullable;

	public CompositeFetch(
			NavigablePath navigablePath,
			EmbeddableValuedModelPart embeddedPartDescriptor,
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			boolean nullable,
			DomainResultCreationState creationState) {
		super( embeddedPartDescriptor, navigablePath );

		this.fetchParent = fetchParent;
		this.fetchTiming = fetchTiming;
		this.nullable = nullable;

		creationState.getSqlAstCreationState().getFromClauseAccess().registerTableGroup(
				getNavigablePath(),
				creationState.getSqlAstCreationState().getFromClauseAccess().findTableGroup( fetchParent.getNavigablePath() )
		);

		afterInitialize( creationState );
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public EmbeddableValuedModelPart getFetchContainer() {
		return (EmbeddableValuedModelPart) super.getFetchContainer();
	}

	@Override
	public EmbeddableValuedModelPart getReferencedMappingContainer() {
		return getFetchContainer();
	}

	@Override
	public Fetchable getFetchedMapping() {
		return getFetchContainer();
	}

	@Override
	public EmbeddableMappingType getReferencedMappingType() {
		return getFetchContainer().getEmbeddableTypeDescriptor();
	}

	@Override
	public boolean isNullable() {
		return nullable;
	}

	@Override
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationState creationState) {
		final CompositeFetchInitializer initializer = new CompositeFetchInitializer(
				parentAccess,
				this,
				collector,
				creationState
		);


		collector.accept( initializer );

		return new CompositeAssembler( initializer );
	}
}
